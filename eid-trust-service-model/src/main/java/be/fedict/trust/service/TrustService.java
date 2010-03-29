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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CRLException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.tsp.TSPException;

import be.fedict.trust.service.entity.WSSecurityConfigEntity;
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
	 * @param certificateChain
	 */
	ValidationResult validate(List<X509Certificate> certificateChain);

	/**
	 * Checks whether the given certificate chain is valid.
	 * 
	 * @param trustDomain
	 *            optional, can be null. If so default trust domain is taken.
	 * @param certificateChain
	 * @param returnRevocationData
	 *            if true, used revocation data will be filled in in the
	 *            {@link ValidationResult} and no caching will be used.
	 * 
	 * @throws TrustDomainNotFoundException
	 */
	ValidationResult validate(String trustDomain,
			List<X509Certificate> certificateChain, boolean returnRevocationDate)
			throws TrustDomainNotFoundException;

	/**
	 * Checks whether the given certificate chain was valid at the specified
	 * {@link Date}, using the specified revocation data.
	 * 
	 * @param trustDomain
	 * @param certificateChain
	 * @param validationDate
	 * @param ocspResponses
	 * @param crls
	 * 
	 * @throws TrustDomainNotFoundException
	 * @throws IOException
	 * @throws CRLException
	 * @throws NoSuchProviderException
	 * @throws CertificateException
	 */
	ValidationResult validate(String trustDomain,
			List<X509Certificate> certificateChain, Date validationDate,
			List<byte[]> ocspResponses, List<byte[]> crls)
			throws TrustDomainNotFoundException, CertificateException,
			NoSuchProviderException, CRLException, IOException;

	/**
	 * Validate the specified encoded Timestamp Token
	 * 
	 * @param timestampToken
	 * @throws CMSException
	 * @throws IOException
	 * @throws TSPException
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 * @throws CertStoreException
	 */
	ValidationResult validateTimestamp(byte[] timestampToken)
			throws TSPException, IOException, CMSException,
			NoSuchAlgorithmException, NoSuchProviderException,
			CertStoreException;

	/**
	 * Returns the {@link WSSecurityConfigEntity}.
	 */
	WSSecurityConfigEntity getWsSecurityConfig();

}
