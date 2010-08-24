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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
            handleClockDriftTimeout();
        } else {
            handleTrustPointTimeout(timerInfo);
        }
    }

    private void handleClockDriftTimeout() {

        // perform clock drift detection
        this.clockDriftService.execute();
    }

    private void handleTrustPointTimeout(String name) {

        TrustPointEntity trustPoint = this.entityManager.find(
                TrustPointEntity.class, name);
        if (null == trustPoint) {
            LOG.warn("unknown trust point: " + name);
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
    }

    /**
     * {@inheritDoc}
     */
    public void startTimer(ClockDriftConfigEntity clockDriftConfig
    ) {
        LOG.debug("start timer for clock drift detection");

        if (0 == clockDriftConfig.getClockDriftInterval()) {
            LOG.debug("no interval set for clock drift, ignoring...");
            return;
        }

        // remove old timers
        cancelTimers(TrustServiceConstants.CLOCK_DRIFT_TIMER);

        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(TrustServiceConstants.CLOCK_DRIFT_TIMER);
        timerConfig.setPersistent(true);

        long interval = 1000 * 60 * clockDriftConfig.getClockDriftInterval();

        Timer timer = this.timerService.createIntervalTimer(new Date().getTime() + interval,
                interval, timerConfig);

        LOG.debug("created timer for clock drift at " + timer.getNextTimeout().toString());
        clockDriftConfig.setFireDate(timer.getNextTimeout());
    }

    /**
     * {@inheritDoc}
     */
    public void startTimer(TrustPointEntity trustPoint) {

        LOG.debug("start timer for " + trustPoint.getName());

        if (0 == trustPoint.getCrlRefreshInterval()) {
            LOG.debug("no CRL refresh set for trust point "
                    + trustPoint.getName() + " ignoring...");
            return;
        }

        // remove old timers
        cancelTimers(trustPoint.getName());

        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(trustPoint.getName());
        timerConfig.setPersistent(true);

        long interval = 1000 * 60 * trustPoint.getCrlRefreshInterval();

        Timer timer = this.timerService.createIntervalTimer(new Date().getTime() +
                interval, interval, timerConfig);

        LOG.debug("created timer for trustpoint " + trustPoint.getName()
                + " at " + timer.getNextTimeout().toString());
        trustPoint.setFireDate(timer.getNextTimeout());
    }

    /**
     * {@inheritDoc}
     */
    public void startTimerNow(TrustPointEntity trustPoint) {

        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(trustPoint.getName());
        timerConfig.setPersistent(true);

        Timer timer = this.timerService.createSingleActionTimer(1000 * 10, timerConfig);

        LOG.debug("created single action timer for trustpoint " + trustPoint.getName()
                + " at " + timer.getNextTimeout().toString());
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
