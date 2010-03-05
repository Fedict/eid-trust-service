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

import be.fedict.trust.service.entity.AdminEntity;
import be.fedict.trust.service.exception.RemoveLastAdminException;

/**
 * Administrator service.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface AdministratorService {

	/**
	 * List all {@link AdminEntity}'s.
	 */
	List<AdminEntity> listAdmins();

	/**
	 * Register the specified authentication certificate chain as administrator.
	 * Does a basic public key verification.
	 * 
	 * @param authnCert
	 * @return the created {@link AdminEntity}
	 */
	AdminEntity register(X509Certificate authnCert);

	/**
	 * Register the pending {@link AdminEntity}
	 * 
	 * @param admin
	 */
	void register(AdminEntity admin);

	/**
	 * Removes the selected administrator. If only 1 administrator remains,
	 * throws {@link RemoveLastAdminException}.
	 * 
	 * @param admin
	 * 
	 * @throws RemoveLastAdminException
	 */
	void remove(AdminEntity admin) throws RemoveLastAdminException;
}
