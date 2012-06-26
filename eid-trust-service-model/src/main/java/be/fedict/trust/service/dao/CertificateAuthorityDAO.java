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

package be.fedict.trust.service.dao;

import java.math.BigInteger;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.security.auth.x500.X500Principal;

import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.RevokedCertificateEntity;
import be.fedict.trust.service.entity.TrustPointEntity;

/**
 * Certificate Authority DAO.
 * 
 * @author wvdhaute
 */
@Local
public interface CertificateAuthorityDAO {

	/**
	 * Return {@link CertificateAuthorityEntity} from specified name. Returns
	 * <code>null</code> if not found.
	 */
	CertificateAuthorityEntity findCertificateAuthority(String name);

	/**
	 * Create a new {@link CertificateAuthorityEntity}.
	 */
	CertificateAuthorityEntity addCertificateAuthority(
			X509Certificate certificate, String crlUrl);

	/**
	 * Returns {@link CertificateAuthorityEntity} from the specified
	 * {@link X509Certificate}. Returns <code>null</code> if not found.
	 */
	CertificateAuthorityEntity findCertificateAuthority(
			X509Certificate certificate);

	/**
	 * Remove {@link CertificateAuthorityEntity}'s related to the specified
	 * {@link TrustPointEntity}.
	 */
	void removeCertificateAuthorities(TrustPointEntity trustPoint);

	void removeCertificateAuthority(CertificateAuthorityEntity ca);

	/**
	 * Add a {@link RevokedCertificateEntity} entry.
	 */
	RevokedCertificateEntity addRevokedCertificate(String issuerName,
			BigInteger serialNumber, Date revocationDate, BigInteger crlNumber);

	/**
	 * Remove {@link RevokedCertificateEntity}'s for the specified issuer that
	 * are older then the specified crl number.
	 * 
	 * @return # of {@link RevokedCertificateEntity}'s removed.
	 */
	int removeOldRevokedCertificates(BigInteger crlNumber, String issuerName);

	/**
	 * Persist batch of {@link X509CRLEntry} to the database.
	 */
	void updateRevokedCertificates(Set<X509CRLEntry> revokedCertificates,
			BigInteger crlNumber, X500Principal crlIssuer);

	/**
	 * Persist a batch of CRL entries to the database given the map of already
	 * loaded database entities.
	 * 
	 * @param revokedCertificates
	 * @param crlNumber
	 * @param crlIssuer
	 * @param revokedCertificateEntities
	 */
	void updateRevokedCertificates(Set<X509CRLEntry> revokedCertificates,
			BigInteger crlNumber, X500Principal crlIssuer,
			Map<String, RevokedCertificateEntity> revokedCertificatesMap);

	/**
	 * Remove all {@link RevokedCertificateEntity}'s for specified issuer.
	 */
	int removeRevokedCertificates(String issuerName);

	List<RevokedCertificateEntity> getRevokedCertificates(String issuerName);

	/**
	 * @param issuerName
	 *            the issuer to search for.
	 * @return the highest CRL number available in the cache for specified
	 *         issuer or <code>null</code> if no cache entries exist.
	 */
	BigInteger findCrlNumber(String issuerName);

	List<CertificateAuthorityEntity> listCertificateAuthorities();

	long getTotalCachedCertificates();

	long getTotalActiveCachedCAs();

	/**
	 * Gives back a list of all certificate authorities that have an active CRL
	 * cache.
	 * 
	 * @return
	 */
	List<CertificateAuthorityEntity> listActiveCertificateAuthorities();
}
