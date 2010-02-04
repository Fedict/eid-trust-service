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

import java.security.cert.X509Certificate;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.trust.service.AdminAuthorizationService;
import be.fedict.trust.service.dao.AdministratorDAO;
import be.fedict.trust.service.entity.AdminEntity;

/**
 * Administrator Authorization Service Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
@LocalBinding(jndiBinding = AdminAuthorizationService.JNDI_BINDING)
public class AdminAuthorizationServiceBean implements AdminAuthorizationService {

	private static final Log LOG = LogFactory
			.getLog(AdminAuthorizationServiceBean.class);

	@EJB
	private AdministratorDAO administratorDAO;

	/**
	 * {@inheritDoc}
	 */
	public String authenticate(X509Certificate authnCert) {

		LOG.debug("authenticate");

		// lookup admin entity
		AdminEntity admin = this.administratorDAO.findAdmin(authnCert
				.getPublicKey());
		if (null == admin) {
			if (this.administratorDAO.listAdmins().isEmpty()) {
				// register initial administrator
				LOG.debug("register initial admin");
				admin = this.administratorDAO.addAdmin(authnCert);
				LOG.debug("initial admin: " + admin.getId());
				return admin.getId();
			}
		} else {
			return admin.getId();
		}

		LOG.error("administrator not found");
		return null;
	}
}
