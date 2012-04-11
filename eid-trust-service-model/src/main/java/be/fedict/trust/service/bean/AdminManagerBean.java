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

import java.security.cert.X509Certificate;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import be.fedict.trust.service.AdminManager;
import be.fedict.trust.service.dao.AdministratorDAO;
import be.fedict.trust.service.entity.AdministratorEntity;

/**
 * Administrator Service Bean implementation.
 * 
 * @author wvdhaute
 */
@Stateless
public class AdminManagerBean implements AdminManager {

	@EJB
	private AdministratorDAO administratorDAO;

	public boolean isAdmin(X509Certificate certificate) {

		AdministratorEntity adminEntity = this.administratorDAO
				.findAdmin(certificate);
		if (null != adminEntity) {
			if (adminEntity.isPending()) {
				// still awaiting approval.
				return false;
			}
			return true;
		}

		if (!this.administratorDAO.listAdmins().isEmpty()) {
			/*
			 * We register a 'pending' admin.
			 */
			this.administratorDAO.addAdmin(certificate, true);
			return false;
		}

		/*
		 * Else we bootstrap the admin.
		 */
		this.administratorDAO.addAdmin(certificate, false);
		return true;
	}
}
