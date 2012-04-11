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

import java.util.Collection;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.Depends;

import be.fedict.trust.service.ClockDriftService;
import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.dao.AuditDAO;
import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.entity.Status;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;

/**
 * Scheduler Service Bean implementation.
 * 
 * @author wvdhaute
 */
@Stateless
@Depends("org.hornetq:module=JMS,name=\"" + HarvesterMDB.HARVESTER_QUEUE_NAME
		+ "\",type=Queue")
public class SchedulingServiceBean implements SchedulingService {

	private static final Log LOG = LogFactory
			.getLog(SchedulingServiceBean.class);

	@Resource
	private TimerService timerService;

	@Resource(mappedName = "java:JmsXA")
	private QueueConnectionFactory queueConnectionFactory;

	@Resource(mappedName = HarvesterMDB.HARVESTER_QUEUE_LOCATION)
	private Queue queue;

	@EJB
	private TrustDomainDAO trustDomainDAO;

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
					LOG.debug("harvester notified for "
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
	public void startTimer(ClockDriftConfigEntity clockDriftConfig)
			throws InvalidCronExpressionException {
		LOG.debug("start timer for clock drift detection");

		if (null == clockDriftConfig.getCronSchedule()
				|| clockDriftConfig.getCronSchedule().isEmpty()) {
			LOG.debug("no interval set for clock drift, ignoring...");
			return;
		}

		// remove old timers
		cancelTimers(TrustServiceConstants.CLOCK_DRIFT_TIMER);

		TimerConfig timerConfig = new TimerConfig();
		timerConfig.setInfo(TrustServiceConstants.CLOCK_DRIFT_TIMER);
		timerConfig.setPersistent(false);

		ScheduleExpression schedule = getScheduleExpression(clockDriftConfig
				.getCronSchedule());

		Timer timer;
		try {
			timer = this.timerService
					.createCalendarTimer(schedule, timerConfig);
		} catch (Exception e) {
			LOG.error(
					"Exception while creating timer for clock drift: "
							+ e.getMessage(), e);
			throw new InvalidCronExpressionException(e);
		}

		LOG.debug("created timer for clock drift at "
				+ timer.getNextTimeout().toString());
		clockDriftConfig.setFireDate(timer.getNextTimeout());
	}

	/**
	 * {@inheritDoc}
	 */
	public void startTimer(TrustPointEntity trustPoint)
			throws InvalidCronExpressionException {

		LOG.debug("start timer for " + trustPoint.getName());

		if (null == trustPoint.getCrlRefreshCronSchedule()
				|| trustPoint.getCrlRefreshCronSchedule().isEmpty()) {
			LOG.debug("no CRL refresh set for trust point "
					+ trustPoint.getName() + " ignoring...");
			return;
		}

		// remove old timers
		cancelTimers(trustPoint.getName());

		TimerConfig timerConfig = new TimerConfig();
		timerConfig.setInfo(trustPoint.getName());
		timerConfig.setPersistent(false);

		ScheduleExpression schedule = getScheduleExpression(trustPoint
				.getCrlRefreshCronSchedule());

		Timer timer;
		try {
			timer = this.timerService
					.createCalendarTimer(schedule, timerConfig);
		} catch (Exception e) {
			LOG.error(
					"Exception while creating timer for clock drift: "
							+ e.getMessage(), e);
			throw new InvalidCronExpressionException(e);
		}

		LOG.debug("created timer for trustpoint " + trustPoint.getName()
				+ " at " + timer.getNextTimeout().toString());
		trustPoint.setFireDate(timer.getNextTimeout());
	}

	private ScheduleExpression getScheduleExpression(String cronSchedule) {

		ScheduleExpression schedule = new ScheduleExpression();
		String[] fields = cronSchedule.split(" ");
		if (fields.length > 8) {
			throw new IllegalArgumentException(
					"Too many fields in cronexpression: " + cronSchedule);
		}
		if (fields.length > 1) {
			schedule.second(fields[0]);
		}
		if (fields.length > 2) {
			schedule.minute(fields[1]);
		}
		if (fields.length > 3) {
			schedule.hour(fields[2]);
		}
		if (fields.length > 4) {
			schedule.dayOfMonth(fields[3]);
		}
		if (fields.length > 5) {
			schedule.month(fields[4]);
		}
		if (fields.length > 6) {
			schedule.dayOfWeek(fields[5]);
		}
		if (fields.length > 7) {
			schedule.year(fields[6]);
		}

		return schedule;
	}

	/**
	 * {@inheritDoc}
	 */
	public void startTimerNow(TrustPointEntity trustPoint) {

		TimerConfig timerConfig = new TimerConfig();
		timerConfig.setInfo(trustPoint.getName());
		timerConfig.setPersistent(false);

		Timer timer = this.timerService.createSingleActionTimer(1000 * 10,
				timerConfig);

		LOG.debug("created single action timer for trustpoint "
				+ trustPoint.getName() + " at "
				+ timer.getNextTimeout().toString());
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
