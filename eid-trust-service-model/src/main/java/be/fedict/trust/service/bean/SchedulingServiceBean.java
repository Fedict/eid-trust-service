/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2010 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.trust.service.bean;

import be.fedict.trust.service.ClockDriftService;
import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.dao.AuditDAO;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.entity.Status;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.CronTrigger;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.jms.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.Date;

/**
 * Scheduler Service Bean implementation.
 *
 * @author wvdhaute
 */
@Stateless
public class SchedulingServiceBean implements SchedulingService {

    private static final Log LOG = LogFactory
            .getLog(SchedulingServiceBean.class);

    @Resource
    private TimerService timerService;

    @Resource(mappedName = "java:JmsXA")
    private QueueConnectionFactory queueConnectionFactory;

    @Resource(mappedName = HarvesterMDB.HARVESTER_QUEUE_NAME)
    private Queue queue;

    @EJB
    private TrustDomainDAO trustDomainDAO;

    @EJB
    private ConfigurationDAO configurationDAO;

    @EJB
    private ClockDriftService clockDriftService;

    @EJB
    private AuditDAO auditDAO;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * {@inheritDoc}
     */
    @Timeout
    public void timeOut(Timer timer) {

        String timerInfo = (String) timer.getInfo();
        if (null == timerInfo) {
            LOG.error("no timer info ?? cancel timer");
            timer.cancel();
            return;
        }

        LOG.debug("scheduler timeout for: " + timerInfo);
        if (timerInfo.equals(TrustServiceConstants.CLOCK_DRIFT_TIMER)) {
            handleClockDriftTimeout(timer);
        } else {
            handleTrustPointTimeout(timerInfo, timer);
        }
    }

    private void handleClockDriftTimeout(Timer timer) {

        ClockDriftConfigEntity clockDriftConfig = this.configurationDAO
                .getClockDriftConfig();

        // the clock drift apparently has another timer still running
        // we just return without setting this timer again
        if (!clockDriftConfig.getTimerHandle().equals(timer.getHandle())) {
            LOG.debug("Ignoring duplicate timer for clock drift");
            return;
        }

        // perform clock drift detection
        this.clockDriftService.execute();

        // start timer
        try {
            startTimer(clockDriftConfig, true);
        } catch (InvalidCronExpressionException e) {
            this.auditDAO.logAudit("Exception starting timer for clock drift");
        }
    }

    private void handleTrustPointTimeout(String name, Timer timer) {

        TrustPointEntity trustPoint = this.entityManager.find(
                TrustPointEntity.class, name);
        if (null == trustPoint) {
            LOG.warn("unknown trust point: " + name);
            return;
        }

        // the trustPoint apparently has another timer still running
        // we just return without setting this timer again
        if (!trustPoint.getTimerHandle().equals(timer.getHandle())) {
            LOG.debug("Ignoring duplicate timer for: " + trustPoint.getName());
            return;
        }

        // notify harvester
        for (CertificateAuthorityEntity certificateAuthority : this.trustDomainDAO
                .listCertificateAuthorities(trustPoint)) {
            try {
                if (!certificateAuthority.getStatus().equals(Status.PROCESSING)) {
                    notifyHarvester(certificateAuthority.getName());
                    LOG.debug("harvester notified for"
                            + certificateAuthority.getName());
                }
            } catch (JMSException e) {
                this.auditDAO.logAudit("Failed to notify harvester for CA="
                        + certificateAuthority.getName());
            }
        }

        // start timer
        try {
            startTimer(trustPoint, true);
        } catch (InvalidCronExpressionException e) {
            this.auditDAO.logAudit("Exception starting timer for trust point: "
                    + trustPoint.getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void startTimer(ClockDriftConfigEntity clockDriftConfig,
                           boolean update) throws InvalidCronExpressionException {
        LOG.debug("start timer for clock drift detection");

        if (null == clockDriftConfig.getCron()) {
            LOG.debug("no CRL refresh set for clock drift, ignoring...");
            return;
        }

        Date fireDate = getFireDate(clockDriftConfig.getCron(), update,
                clockDriftConfig.getFireDate());

        // remove old timers
        cancelTimers(TrustServiceConstants.CLOCK_DRIFT_TIMER);

        Timer timer = this.timerService.createTimer(fireDate,
                TrustServiceConstants.CLOCK_DRIFT_TIMER);
        LOG.debug("created timer for clock drift at " + fireDate.toString());
        clockDriftConfig.setFireDate(fireDate);
        clockDriftConfig.setTimerHandle(timer.getHandle());
    }

    /**
     * {@inheritDoc}
     */
    public void startTimer(TrustPointEntity trustPoint, boolean update)
            throws InvalidCronExpressionException {

        LOG.debug("start timer for " + trustPoint.getName());

        if (null == trustPoint.getCrlRefreshCron()) {
            LOG.debug("no CRL refresh set for trust point "
                    + trustPoint.getName() + " ignoring...");
            return;
        }

        Date fireDate = getFireDate(trustPoint.getCrlRefreshCron(), update,
                trustPoint.getFireDate());

        startTimer(trustPoint, fireDate);
    }

    /**
     * {@inheritDoc}
     */
    public void startTimerNow(TrustPointEntity trustPoint) {

        Date fireDate = new Date(new Date().getTime() + (1000 * 10));
        startTimer(trustPoint, fireDate);
    }

    private void startTimer(TrustPointEntity trustPoint, Date fireDate) {

        // remove old timers
        cancelTimers(trustPoint.getName());

        Timer timer = this.timerService.createTimer(fireDate, trustPoint
                .getName());
        LOG.debug("created timer for " + trustPoint.getName() + " at "
                + fireDate.toString());
        trustPoint.setFireDate(fireDate);
        trustPoint.setTimerHandle(timer.getHandle());
        this.entityManager.flush();
    }

    private Date getFireDate(String cron, boolean update, Date prevFireDate)
            throws InvalidCronExpressionException {
        CronTrigger cronTrigger;
        try {
            cronTrigger = new CronTrigger("name", "group", cron);
        } catch (Exception e) {
            LOG.error("invalid cron expression");
            throw new InvalidCronExpressionException(e);
        }

        Date fireDate = cronTrigger.computeFirstFireTime(null);
        if (update && fireDate.equals(prevFireDate)) {
            cronTrigger.triggered(null);
            fireDate = cronTrigger.getNextFireTime();
        }
        return fireDate;
    }

    /**
     * {@inheritDoc}
     */
    public void cancelTimers(ClockDriftConfigEntity clockDriftConfig) {
        cancelTimers(TrustServiceConstants.CLOCK_DRIFT_TIMER);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void cancelTimers(String timerInfo) {

        Collection<Timer> timers = this.timerService.getTimers();
        for (Timer timer : timers) {
            if (timer.getInfo() != null) {
                if (timer.getInfo().equals(timerInfo)) {
                    timer.cancel();
                    LOG.debug("cancel timer: " + timerInfo);
                }
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    public void refreshCA(CertificateAuthorityEntity ca) throws JMSException {

        notifyHarvester(ca.getName());
    }

    private void notifyHarvester(String issuerName) throws JMSException {
        QueueConnection queueConnection = this.queueConnectionFactory
                .createQueueConnection();
        try {
            QueueSession queueSession = queueConnection.createQueueSession(
                    true, Session.AUTO_ACKNOWLEDGE);
            try {
                HarvestMessage harvestMessage = new HarvestMessage(issuerName,
                        true);
                QueueSender queueSender = queueSession.createSender(this.queue);
                try {
                    queueSender
                            .send(harvestMessage.getJMSMessage(queueSession));
                } finally {
                    queueSender.close();
                }
            } finally {
                queueSession.close();
            }
        } finally {
            queueConnection.close();
        }
    }

}
