/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2012 FedICT.
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

import java.security.KeyStore.PrivateKeyEntry;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.KeyStoreUtils;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.entity.WSSecurityConfigEntity;
import be.fedict.trust.service.exception.KeyStoreLoadException;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ServiceIdentityManagerBean {

	private static final Log LOG = LogFactory
			.getLog(ServiceIdentityManagerBean.class);

	private PrivateKeyEntry privateKeyEntry;

	@EJB
	private ConfigurationDAO configurationDAO;

	public PrivateKeyEntry getPrivateKeyEntry() {
		return this.privateKeyEntry;
	}

	public void updateServiceIdentity() {
		LOG.debug("updating service identity...");
		WSSecurityConfigEntity securityConfigEntity = this.configurationDAO
				.getWSSecurityConfig();
		if (null == securityConfigEntity.getKeyStorePath()) {
			LOG.debug("no service identity configured");
			this.privateKeyEntry = null;
			return;
		}
		try {
			this.privateKeyEntry = KeyStoreUtils
					.loadPrivateKeyEntry(securityConfigEntity);
			LOG.debug("service identity configured");
		} catch (KeyStoreLoadException e) {
			LOG.error("error loading service identity: " + e.getMessage(), e);
			this.privateKeyEntry = null;
			return;
		}
	}
}
