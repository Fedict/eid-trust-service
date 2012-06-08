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

package be.fedict.trust.service;

import java.math.BigInteger;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.Date;

import javax.ejb.Local;

/**
 * Simple high-performant validation service. To be used by the OCSP Responder
 * front-end.
 * 
 * @author Frank Cornelis
 */
@Local
public interface ValidationService {

	/**
	 * Validate the PKI status of the referred certificate.
	 * 
	 * @param serialNumber
	 *            the certificate serial number.
	 * @param issuerNameHash
	 *            the SHA1 of the issuer name.
	 * @param issuerKeyHash
	 *            the SHA1 of the issuer public key.
	 * @return <code>null</code> if valid, else the revocation date.
	 */
	Date validate(BigInteger serialNumber, byte[] issuerNameHash,
			byte[] issuerKeyHash);

	/**
	 * Gives back the private key for OCSP responder signing.
	 * 
	 * @return
	 */
	PrivateKeyEntry getPrivateKeyEntry();
}
