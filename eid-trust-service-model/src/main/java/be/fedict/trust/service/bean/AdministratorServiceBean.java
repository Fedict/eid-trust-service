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
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.SecurityDomain;

import be.fedict.trust.service.AdministratorService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.dao.AdministratorDAO;
import be.fedict.trust.service.entity.AdminEntity;
import be.fedict.trust.service.exception.RemoveLastAdminException;

/**
 * Administrator Service Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
@SecurityDomain(TrustServiceConstants.ADMIN_SECURITY_DOMAIN)
public class AdministratorServiceBean implements AdministratorService {

	private static final Log LOG = LogFactory
			.getLog(AdministratorServiceBean.class);

	@EJB
	private AdministratorDAO administratorDAO;

	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public List<AdminEntity> listAdmins() {

		return this.administratorDAO.listAdmins();
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public AdminEntity register(X509Certificate authnCert) {

		LOG.debug("register");

		if (null == this.administratorDAO.findAdmin(authnCert.getPublicKey())) {
			return this.administratorDAO.addAdmin(authnCert, false);
		}

		LOG.error("failed to register administrator");
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public void register(AdminEntity admin) {

		LOG.debug("register pending admin");
		AdminEntity attachedAdminEntity = this.administratorDAO
				.attachAdmin(admin);
		attachedAdminEntity.setPending(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public void remove(AdminEntity admin) throws RemoveLastAdminException {

		LOG.debug("remove admin: " + admin.getName());

		// check not last administrator
		if (listAdmins().size() == 1) {
			LOG.error("cannot remove last administrator");
			throw new RemoveLastAdminException();
		}

		// remove
		this.administratorDAO.removeAdmin(admin);
	}
}
