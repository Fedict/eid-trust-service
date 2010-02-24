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

import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.Local;

import be.fedict.trust.service.exception.TrustDomainNotFoundException;

/**
 * Trust Service interface.
 * 
 * @author fcorneli
 * 
 */
@Local
public interface TrustService {

	/**
	 * Checks whether the given authentication certificate chain is valid.
	 * 
	 * @param authenticationCertificateChain
	 * @return
	 */
	boolean isValid(List<X509Certificate> authenticationCertificateChain);

	/**
	 * Checks whether the given authentication certificate chain is valid.
	 * 
	 * @param trustDomain
	 *            optional, can be null. If so default trust domain is taken.
	 * @param authenticationCertificateChain
	 * @return
	 * @throws TrustDomainNotFoundException
	 */
	boolean isValid(String trustDomain,
			List<X509Certificate> authenticationCertificateChain)
			throws TrustDomainNotFoundException;
}
