/*
 * eID Trust Service Project.
 * Copyright (C) 2009 FedICT.
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
import java.util.Date;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
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
import org.quartz.CronTrigger;

import be.fedict.trust.service.ClockDriftService;
import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.TimerInfo;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;

/**
 * Scheduler Service Bean implementation.
 * 
 * @author wvdhaute
 * 
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

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	@Timeout
	public void timeOut(Timer timer) {

		TimerInfo timerInfo = (TimerInfo) timer.getInfo();
		if (null == timerInfo) {
			LOG.error("no timer info ?? cancel timer");
			timer.cancel();
			return;
		}

		LOG.debug("scheduler timeout for: " + timerInfo.getType() + " name="
				+ timerInfo.getName());

		switch (timerInfo.getType()) {
		case TRUST_DOMAIN: {
			handleTrustDomainTimeout(timerInfo.getName(), timer);
			break;
		}
		case TRUST_POINT: {
			handleTrustPointTimeout(timerInfo.getName(), timer);
			break;
		}
		case CLOCK_DRIFT: {
			handleClockDriftTimeout(timer);
			break;
		}
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
		clockDriftService.execute();

		// start timer
		try {
			startTimer(clockDriftConfig, true);
		} catch (InvalidCronExpressionException e) {
			LOG.error("Exception starting timer for clock drift");
			return;
			// XXX: audit ?
		}
	}

	private void handleTrustDomainTimeout(String name, Timer timer) {

		TrustDomainEntity trustDomain = this.entityManager.find(
				TrustDomainEntity.class, name);
		if (null == trustDomain) {
			LOG.warn("unknown trustDomain: " + name);
			return;
		}

		// the trustDomain apparently has another timer still running
		// we just return without setting this timer again
		if (!trustDomain.getTimerHandle().equals(timer.getHandle())) {
			LOG.debug("Ignoring duplicate timer for: " + trustDomain.getName());
			return;
		}

		// notify harvester
		for (TrustPointEntity trustPoint : this.trustDomainDAO
				.listTrustPoints(trustDomain)) {
			for (CertificateAuthorityEntity certificateAuthority : this.trustDomainDAO
					.listCertificateAuthorities(trustPoint)) {
				try {
					notifyHarvester(certificateAuthority.getName());
					LOG.debug("harvester notified for"
							+ certificateAuthority.getName());
				} catch (JMSException e) {
					LOG.error("Failed to notify harvester", e);
					// XXX: audit
				}
			}
		}

		// start timer
		try {
			startTimer(trustDomain, true);
		} catch (InvalidCronExpressionException e) {
			LOG.error("Exception starting timer for trust domain: "
					+ trustDomain.getName());
			return;
			// XXX: audit ?
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
				notifyHarvester(certificateAuthority.getName());
				LOG.debug("harvester notified for"
						+ certificateAuthority.getName());
			} catch (JMSException e) {
				LOG.error("Failed to notify harvester", e);
				// XXX: audit
			}
		}

		// start timer
		try {
			startTimer(trustPoint, true);
		} catch (InvalidCronExpressionException e) {
			LOG.error("Exception starting timer for trust point: "
					+ trustPoint.getName());
			return;
			// XXX: audit ?
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
		cancelTimers(new TimerInfo(clockDriftConfig));

		Timer timer = this.timerService.createTimer(fireDate, new TimerInfo(
				clockDriftConfig));
		LOG.debug("created timer for clock drift at " + fireDate.toString());
		clockDriftConfig.setFireDate(fireDate);
		clockDriftConfig.setTimerHandle(timer.getHandle());
	}

	/**
	 * {@inheritDoc}
	 */
	public void startTimer(TrustDomainEntity trustDomain, boolean update)
			throws InvalidCronExpressionException {
		LOG.debug("start timer for " + trustDomain.getName());

		// remove old timers
		cancelTimers(new TimerInfo(trustDomain));

		if (null == trustDomain.getCrlRefreshCron()
				|| trustDomain.getCrlRefreshCron().equals("")) {
			LOG.debug("no CRL refresh set for trust domain "
					+ trustDomain.getName() + " ignoring...");
			return;
		}

		Date fireDate = getFireDate(trustDomain.getCrlRefreshCron(), update,
				trustDomain.getFireDate());

		Timer timer = this.timerService.createTimer(fireDate, new TimerInfo(
				trustDomain));
		LOG.debug("created timer for " + trustDomain.getName() + " at "
				+ fireDate.toString());
		trustDomain.setFireDate(fireDate);
		trustDomain.setTimerHandle(timer.getHandle());
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

		// remove old timers
		cancelTimers(new TimerInfo(trustPoint));

		Timer timer = this.timerService.createTimer(fireDate, new TimerInfo(
				trustPoint));
		LOG.debug("created timer for " + trustPoint.getName() + " at "
				+ fireDate.toString());
		trustPoint.setFireDate(fireDate);
		trustPoint.setTimerHandle(timer.getHandle());
		this.entityManager.flush();
	}

	private Date getFireDate(String cron, boolean update, Date prevFireDate)
			throws InvalidCronExpressionException {
		CronTrigger cronTrigger = null;
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
	@SuppressWarnings("unchecked")
	public void cancelTimers(TimerInfo timerInfo) {

		Collection<Timer> timers = this.timerService.getTimers();
		for (Timer timer : timers) {
			if (timer.getInfo() != null) {
				if (((TimerInfo) timer.getInfo()).equals(timerInfo)) {
					timer.cancel();
					LOG.debug("cancel timer: " + timerInfo.toString());
				}
			}

		}
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
