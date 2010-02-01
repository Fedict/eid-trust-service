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

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.ejb3.annotation.SecurityDomain;

import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.TrustDomainService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;

/**
 * Trust Domain Service Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
@LocalBinding(jndiBinding = TrustDomainService.JNDI_BINDING)
@SecurityDomain(TrustServiceConstants.ADMIN_SECURITY_DOMAIN)
public class TrustDomainServiceBean implements TrustDomainService {

	private static final Log LOG = LogFactory
			.getLog(TrustDomainServiceBean.class);

	@EJB
	private TrustDomainDAO trustDomainDAO;

	@EJB
	private SchedulingService schedulingService;

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public List<TrustDomainEntity> listTrustDomains() {

		LOG.debug("list trust domains");
		return this.trustDomainDAO.listTrustDomains();
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public void save(TrustDomainEntity trustDomain)
			throws InvalidCronExpressionException {

		LOG.debug("save trust domain: " + trustDomain.getName());
		TrustDomainEntity attachedTrustDomain = this.trustDomainDAO
				.findTrustDomain(trustDomain.getName());
		attachedTrustDomain.setCrlRefreshCron(trustDomain.getCrlRefreshCron());
		this.schedulingService.startTimer(attachedTrustDomain);
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public List<TrustPointEntity> listTrustPoints(TrustDomainEntity trustDomain) {

		LOG.debug("list trust points for " + trustDomain.getName());
		return this.trustDomainDAO.listTrustPoints(trustDomain);
	}
}
