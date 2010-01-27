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
import java.util.List;

import javax.annotation.Resource;
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
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;
import org.quartz.CronTrigger;

import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.TrustDomainEntity;
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

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	@Timeout
	public void timeOut(Timer timer) {

		String name = (String) timer.getInfo();
		LOG.debug("scheduler timeout for: " + name);

		TrustDomainEntity trustDomain = this.entityManager.find(
				TrustDomainEntity.class, name);
		if (null == trustDomain) {
			LOG.warn("unknown trustDomain: " + name);
			return;
		}

		// the trustDomain apparently has another timer still running
		// we just return without setting this timer again
		if (!trustDomain.getTimerHandle().equals(timer.getHandle())) {
			LOG.debug("Ignoring duplicate timer for: " + name);
			return;
		}

		// notify harvester for the scheduling's trust domains
		for (CertificateAuthorityEntity certificateAuthority : getCertificateAuthorities(trustDomain)) {
			try {
				notifyHarvester(certificateAuthority.getName());
				LOG.debug("harvester notified");
			} catch (JMSException e) {
				LOG.error("Failed to notify harvester", e);
				// XXX: audit
			}
		}

		try {
			startTimer(trustDomain);
		} catch (InvalidCronExpressionException e) {
			LOG.error("Exception starting timer for trust domain: "
					+ trustDomain.getName());
			return;
			// XXX: audit ?
		}
	}

	@SuppressWarnings("unchecked")
	private List<CertificateAuthorityEntity> getCertificateAuthorities(
			TrustDomainEntity trustDomain) {
		Query query = this.entityManager
				.createQuery("SELECT ca FROM CertificateAuthorityEntity AS ca WHERE ca.trustDomain = :trustDomain");
		query.setParameter("trustDomain", trustDomain);
		return (List<CertificateAuthorityEntity>) query.getResultList();
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
					.getCronExpression());
		} catch (Exception e) {
			LOG.error("invalid cron expression");
			throw new InvalidCronExpressionException(e);
		}
		Date fireDate = cronTrigger.computeFirstFireTime(null);
		if (fireDate.equals(trustDomain.getFireDate())) {
			cronTrigger.triggered(null);
			fireDate = cronTrigger.getNextFireTime();
		}

		Timer timer = this.timerService.createTimer(fireDate, trustDomain
				.getName());
		LOG.debug("created timer for " + trustDomain.getName() + " at "
				+ fireDate.toString());
		trustDomain.setFireDate(fireDate);
		trustDomain.setTimerHandle(timer.getHandle());
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
