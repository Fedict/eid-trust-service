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

import be.fedict.trust.service.AdministratorService;
import be.fedict.trust.service.dao.AdministratorDAO;
import be.fedict.trust.service.entity.AdministratorEntity;
import be.fedict.trust.service.exception.RemoveLastAdminException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Administrator Service Bean implementation.
 * 
 * @author wvdhaute
 */
@Stateless
public class AdministratorServiceBean implements AdministratorService {

	private static final Log LOG = LogFactory
			.getLog(AdministratorServiceBean.class);

	@EJB
	private AdministratorDAO administratorDAO;

	public List<AdministratorEntity> listAdmins() {

		return this.administratorDAO.listAdmins();
	}

	/**
	 * {@inheritDoc}
	 */
	public AdministratorEntity register(X509Certificate authnCert) {

		LOG.debug("register");

		if (null == this.administratorDAO.findAdmin(authnCert)) {
			return this.administratorDAO.addAdmin(authnCert, false);
		}

		LOG.error("failed to register administrator");
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void register(AdministratorEntity admin) {

		LOG.debug("register pending admin");
		AdministratorEntity attachedAdminEntity = this.administratorDAO
				.attachAdmin(admin);
		attachedAdminEntity.setPending(false);
	}

	/**
	 * {@inheritDoc}
	 */
	public void remove(AdministratorEntity admin)
			throws RemoveLastAdminException {

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
