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

package be.fedict.trust.service;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.Local;

import be.fedict.trust.service.entity.AdministratorEntity;
import be.fedict.trust.service.exception.RemoveLastAdminException;

/**
 * Administrator service.
 * 
 * @author wvdhaute
 */
@Local
public interface AdministratorService {

	/**
	 * List all {@link AdministratorEntity}'s.
	 * 
	 * @return list of all administrators
	 */
	List<AdministratorEntity> listAdmins();

	/**
	 * Register the specified authentication certificate chain as administrator.
	 * Does a basic public key verification.
	 * 
	 * @param authnCert
	 *            authentication certificate
	 * @return the created {@link AdministratorEntity}
	 */
	AdministratorEntity register(X509Certificate authnCert);

	/**
	 * Register the pending {@link AdministratorEntity}
	 * 
	 * @param admin
	 *            the pending admin to register
	 */
	void register(AdministratorEntity admin);

	/**
	 * Removes the selected administrator. If only 1 administrator remains,
	 * throws {@link RemoveLastAdminException}.
	 * 
	 * @param admin
	 *            the admin to remove
	 * @throws RemoveLastAdminException
	 *             cannot remove last admin
	 */
	void remove(AdministratorEntity admin) throws RemoveLastAdminException;
}
