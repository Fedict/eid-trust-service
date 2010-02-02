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

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.SecurityDomain;

import be.fedict.trust.service.ConfigurationService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.dao.NetworkConfigDAO;
import be.fedict.trust.service.entity.NetworkConfigEntity;

/**
 * Configuration Service Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
@SecurityDomain(TrustServiceConstants.ADMIN_SECURITY_DOMAIN)
public class ConfigurationServiceBean implements ConfigurationService {

	private static final Log LOG = LogFactory
			.getLog(ConfigurationServiceBean.class);

	@EJB
	private NetworkConfigDAO networkConfigDAO;

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public NetworkConfigEntity getNetworkConfig() {

		return this.networkConfigDAO.getNetworkConfigEntity();
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public void saveNetworkConfig(String proxyHost, int proxyPort,
			boolean enabled) {

		LOG.debug("save network config");
		this.networkConfigDAO.setNetworkConfig(proxyHost, proxyPort);
		this.networkConfigDAO.setEnabled(enabled);
	}
}
