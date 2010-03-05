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

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.SecurityDomain;

import be.fedict.trust.service.ConfigurationService;
import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.entity.NetworkConfigEntity;
import be.fedict.trust.service.entity.TimeProtocol;
import be.fedict.trust.service.exception.InvalidCronExpressionException;

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
	private ConfigurationDAO configurationDAO;

	@EJB
	private SchedulingService schedulingService;

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public NetworkConfigEntity getNetworkConfig() {

		return this.configurationDAO.getNetworkConfigEntity();
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public void saveNetworkConfig(String proxyHost, int proxyPort,
			boolean enabled) {

		LOG.debug("save network config");
		this.configurationDAO.setNetworkConfig(proxyHost, proxyPort);
		this.configurationDAO.setNetworkConfigEnabled(enabled);
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public ClockDriftConfigEntity getClockDriftDetectionConfig() {

		return this.configurationDAO.getClockDriftConfig();
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public void saveClockDriftConfig(TimeProtocol timeProtocol, String server,
			int timeout, int maxClockOffset, String cron)
			throws InvalidCronExpressionException {

		LOG.debug("save clock drift detection config");
		ClockDriftConfigEntity clockDriftConfig = this.configurationDAO
				.setClockDriftConfig(timeProtocol, server, timeout,
						maxClockOffset, cron);
		this.schedulingService.startTimer(clockDriftConfig, false);
	}
}
