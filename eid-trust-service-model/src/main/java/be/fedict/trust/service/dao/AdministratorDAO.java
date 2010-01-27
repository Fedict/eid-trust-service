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

package be.fedict.trust.service.dao;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.Local;

import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.entity.AdminEntity;

/**
 * Administrator DAO.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface AdministratorDAO {

	public static final String JNDI_BINDING = TrustServiceConstants.JNDI_CONTEXT
			+ "/AdministratorDAOBean";

	/**
	 * Returns list of registered administrators.
	 */
	List<AdminEntity> listAdmins();

	/**
	 * Returns {@link AdminEntity} matching the specified {@link PublicKey}.
	 * Returns <code>null</code> if not found.
	 * 
	 * @param publicKey
	 */
	AdminEntity findAdmin(PublicKey publicKey);

	/**
	 * Add new {@link AdminEntity}
	 * 
	 * @param authnCertificate
	 */
	AdminEntity addAdmin(X509Certificate authnCertificate);
}
