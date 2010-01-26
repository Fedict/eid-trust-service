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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.trust.service.InitializationService;
import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.entity.SchedulingEntity;
import be.fedict.trust.service.entity.TrustDomainEntity;

/**
 * Initialization Service Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
@LocalBinding(jndiBinding = InitializationService.JNDI_BINDING)
public class InitializationServiceBean implements InitializationService {

	private static final Log LOG = LogFactory
			.getLog(InitializationServiceBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	@EJB
	private SchedulingService schedulingService;

	public void initialize() {

		LOG.debug("initialize");

		// Default scheduling
		SchedulingEntity scheduling = this.entityManager.find(
				SchedulingEntity.class, "default");
		if (null == scheduling) {
			LOG.debug("create default scheduling");
			scheduling = new SchedulingEntity("default",
					TrustServiceConstants.DEFAULT_CRON);
			entityManager.persist(scheduling);
		}

		// Belgian eID trust domain
		TrustDomainEntity beidTrustDomain = this.entityManager.find(
				TrustDomainEntity.class,
				TrustServiceConstants.BELGIAN_EID_TRUST_DOMAIN);
		if (null == beidTrustDomain) {
			LOG.debug("create Belgian eID trust domain");
			beidTrustDomain = new TrustDomainEntity(
					TrustServiceConstants.BELGIAN_EID_TRUST_DOMAIN, scheduling);
			entityManager.persist(beidTrustDomain);
		}

		// Start default scheduling timer
		LOG.debug("start scheduling " + scheduling.getName());
		schedulingService.startTimer(scheduling);
	}
}
