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

package be.fedict.trust.service;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.ejb.Local;

import be.fedict.trust.service.entity.AdminEntity;

/**
 * Admin authorization service.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface AdminAuthorizationService {

	public static final String JNDI_BINDING = TrustServiceConstants.JNDI_CONTEXT
			+ "/AdminAuthorizationServiceBean";

	/**
	 * Authenticate the specified authentication certificate chain. Does a basic
	 * public key verification and looks up if an {@link AdminEntity} matching
	 * the public key.
	 * 
	 * @param authnCertChain
	 * @return id The {@link AdminEntity}'s id.
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws CertPathValidatorException
	 */
	String authenticate(List<X509Certificate> authnCertChain)
			throws NoSuchAlgorithmException, InvalidKeySpecException,
			CertPathValidatorException;

}
