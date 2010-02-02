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

package be.fedict.trust.service.dao.bean;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.NetworkConfig;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.dao.NetworkConfigDAO;
import be.fedict.trust.service.entity.NetworkConfigEntity;

/**
 * Network Config DAO Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
public class NetworkConfigDAOBean implements NetworkConfigDAO {

	private static final Log LOG = LogFactory
			.getLog(NetworkConfigDAOBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	public NetworkConfigEntity getNetworkConfigEntity() {

		LOG.debug("get network config entity");
		NetworkConfigEntity networkConfig = this.entityManager
				.find(NetworkConfigEntity.class,
						TrustServiceConstants.NETWORK_CONFIG);
		if (null == networkConfig) {
			networkConfig = new NetworkConfigEntity(
					TrustServiceConstants.NETWORK_CONFIG, null, 0);
			this.entityManager.persist(networkConfig);
		}
		return networkConfig;
	}

	/**
	 * {@inheritDoc}
	 */
	public NetworkConfig getNetworkConfig() {
		LOG.debug("get network config entity");
		NetworkConfigEntity networkConfig = getNetworkConfigEntity();
		if (networkConfig.isEnabled()) {
			return new NetworkConfig(networkConfig.getProxyHost(),
					networkConfig.getProxyPort());
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setEnabled(boolean enabled) {

		LOG.debug("set network config enabled: " + enabled);
		NetworkConfigEntity networkConfig = getNetworkConfigEntity();
		networkConfig.setEnabled(enabled);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetworkConfigEntity setNetworkConfig(String proxyHost, int proxyPort) {

		LOG.debug("set network config: proxyHost=" + proxyHost + " proxyPort="
				+ proxyPort);
		NetworkConfigEntity networkConfigEntity = getNetworkConfigEntity();
		networkConfigEntity.setProxyHost(proxyHost);
		networkConfigEntity.setProxyPort(proxyPort);
		return networkConfigEntity;
	}
}
