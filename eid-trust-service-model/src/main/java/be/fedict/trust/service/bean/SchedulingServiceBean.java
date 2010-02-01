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
import org.jboss.ejb3.annotation.LocalBinding;
import org.quartz.CronTrigger;

import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.TimerInfo;
import be.fedict.trust.service.TimerInfo.Type;
import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
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
@LocalBinding(jndiBinding = SchedulingService.JNDI_BINDING)
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

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	@Timeout
	public void timeOut(Timer timer) {

		TimerInfo timerInfo = (TimerInfo) timer.getInfo();
		if (null == timerInfo) {
			LOG.error("no timer info ??");
			return;
		}

		LOG.debug("scheduler timeout for: " + timerInfo.getType() + " name="
				+ timerInfo.getName());

		if (timerInfo.getType().equals(Type.TRUST_DOMAIN)) {
			handleTrustDomainTimeout(timerInfo.getName(), timer);
		} else {
			handleTrustPointTimeout(timerInfo.getName(), timer);
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
					LOG.debug("harvester notified");
				} catch (JMSException e) {
					LOG.error("Failed to notify harvester", e);
					// XXX: audit
				}
			}
		}

		// start timer
		try {
			startTimer(trustDomain);
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
				LOG.debug("harvester notified");
			} catch (JMSException e) {
				LOG.error("Failed to notify harvester", e);
				// XXX: audit
			}
		}

		// start timer
		try {
			startTimer(trustPoint);
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
	public void startTimer(TrustDomainEntity trustDomain)
			throws InvalidCronExpressionException {
		LOG.debug("start timer for " + trustDomain.getName());

		CronTrigger cronTrigger = null;
		try {
			cronTrigger = new CronTrigger("name", "group", trustDomain
					.getCrlRefreshCron());
		} catch (Exception e) {
			LOG.error("invalid cron expression");
			throw new InvalidCronExpressionException(e);
		}
		Date fireDate = cronTrigger.computeFirstFireTime(null);
		if (fireDate.equals(trustDomain.getFireDate())) {
			cronTrigger.triggered(null);
			fireDate = cronTrigger.getNextFireTime();
		}

		Timer timer = this.timerService.createTimer(fireDate, new TimerInfo(
				Type.TRUST_DOMAIN, trustDomain.getName()));
		LOG.debug("created timer for " + trustDomain.getName() + " at "
				+ fireDate.toString());
		trustDomain.setFireDate(fireDate);
		trustDomain.setTimerHandle(timer.getHandle());
		this.entityManager.flush();
	}

	/**
	 * {@inheritDoc}
	 */
	public void startTimer(TrustPointEntity trustPoint)
			throws InvalidCronExpressionException {
		LOG.debug("start timer for " + trustPoint.getName());

		if (null == trustPoint.getCrlRefreshCron()) {
			LOG.debug("no CRL refresh set for trust point "
					+ trustPoint.getName() + " ignoring...");
			return;
		}

		CronTrigger cronTrigger = null;
		try {
			cronTrigger = new CronTrigger("name", "group", trustPoint
					.getCrlRefreshCron());
		} catch (Exception e) {
			LOG.error("invalid cron expression");
			throw new InvalidCronExpressionException(e);
		}
		Date fireDate = cronTrigger.computeFirstFireTime(null);
		if (fireDate.equals(trustPoint.getFireDate())) {
			cronTrigger.triggered(null);
			fireDate = cronTrigger.getNextFireTime();
		}

		Timer timer = this.timerService.createTimer(fireDate, new TimerInfo(
				Type.TRUST_POINT, trustPoint.getName()));
		LOG.debug("created timer for " + trustPoint.getName() + " at "
				+ fireDate.toString());
		trustPoint.setFireDate(fireDate);
		trustPoint.setTimerHandle(timer.getHandle());
		this.entityManager.flush();
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
