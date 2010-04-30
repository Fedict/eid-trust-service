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

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.RevocationData;
import be.fedict.trust.TrustLinker;
import be.fedict.trust.TrustLinkerResult;
import be.fedict.trust.TrustLinkerResultReason;
import be.fedict.trust.crl.CrlTrustLinker;
import be.fedict.trust.service.SnmpConstants;
import be.fedict.trust.service.dao.AuditDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.RevokedCertificateEntity;
import be.fedict.trust.service.entity.Status;
import be.fedict.trust.service.snmp.SNMPInterceptor;

/**
 * Implementation of a trust linker based on the trust service infrastructure.
 * 
 * @author fcorneli
 * 
 */
public class TrustServiceTrustLinker implements TrustLinker {

	private static final Log LOG = LogFactory
			.getLog(TrustServiceTrustLinker.class);

	private final EntityManager entityManager;

	public TrustServiceTrustLinker(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public TrustLinkerResult hasTrustLink(X509Certificate childCertificate,
			X509Certificate certificate, Date validationDate,
			RevocationData revocationData) {

		LOG.debug("certificate: " + childCertificate.getSubjectX500Principal());

		String issuerName = childCertificate.getIssuerX500Principal()
				.toString();
		CertificateAuthorityEntity certificateAuthority = this.entityManager
				.find(CertificateAuthorityEntity.class, issuerName);
		if (null == certificateAuthority) {
			LOG.debug("no data cache entry for CA: " + issuerName);
			/*
			 * Cache Miss
			 */
			SNMPInterceptor.increment(SnmpConstants.CACHE_MISSES,
					SnmpConstants.SNMP_SERVICE, 1L);

			/*
			 * Lookup Root CA's trust point via parent certificates' CA entity.
			 */
			String parentIssuerName = certificate.getIssuerX500Principal()
					.toString();
			CertificateAuthorityEntity parentCertificateAuthority = this.entityManager
					.find(CertificateAuthorityEntity.class, parentIssuerName);
			if (null == parentCertificateAuthority) {
				logAudit("CA not found for " + parentIssuerName);
				LOG.error("CA not found for " + parentIssuerName + " ?!");
				return null;
			}

			// create new CA
			try {
				certificateAuthority = new CertificateAuthorityEntity(
						getCrlUrl(childCertificate), certificate);
				certificateAuthority.setTrustPoint(parentCertificateAuthority
						.getTrustPoint());
			} catch (CertificateEncodingException e) {
				LOG.error("certificate encoding error: " + e.getMessage(), e);
				return null;
			}
			this.entityManager.persist(certificateAuthority);
			return null;
		}
		if (Status.ACTIVE != certificateAuthority.getStatus()) {
			LOG.debug("CA revocation data cache not yet active: " + issuerName);
			/*
			 * Harvester is still busy processing the first CRL.
			 */
			if (null == certificateAuthority.getCrlUrl()) {
				certificateAuthority.setCrlUrl(getCrlUrl(childCertificate));
			}

			if (Status.NONE != certificateAuthority.getStatus()) {
				// none means no CRL is available so not really a cache miss
				SNMPInterceptor.increment(SnmpConstants.CACHE_MISSES,
						SnmpConstants.SNMP_SERVICE, 1L);
			}
			return null;
		}
		/*
		 * Let's use the cached revocation data
		 */
		Date thisUpdate = certificateAuthority.getThisUpdate();
		if (null == thisUpdate) {
			LOG.warn("no thisUpdate value");
			SNMPInterceptor.increment(SnmpConstants.CACHE_MISSES,
					SnmpConstants.SNMP_SERVICE, 1L);
			return null;
		}
		Date nextUpdate = certificateAuthority.getNextUpdate();
		if (null == nextUpdate) {
			LOG.warn("no nextUpdate value");
			SNMPInterceptor.increment(SnmpConstants.CACHE_MISSES,
					SnmpConstants.SNMP_SERVICE, 1L);
			return null;
		}
		/*
		 * First check whether the cached revocation data is up-to-date.
		 */
		if (thisUpdate.after(validationDate)) {
			LOG.warn("cached CRL data too recent");
			SNMPInterceptor.increment(SnmpConstants.CACHE_MISSES,
					SnmpConstants.SNMP_SERVICE, 1L);
			return null;
		}
		if (validationDate.after(nextUpdate)) {
			LOG.warn("cached CRL data too old");
			SNMPInterceptor.increment(SnmpConstants.CACHE_MISSES,
					SnmpConstants.SNMP_SERVICE, 1L);
			return null;
		}
		LOG.debug("using cached CRL data");
		/*
		 * Cache Hit
		 */
		SNMPInterceptor.increment(SnmpConstants.CACHE_HITS,
				SnmpConstants.SNMP_SERVICE, 1L);

		BigInteger serialNumber = childCertificate.getSerialNumber();
		RevokedCertificateEntity revokedCertificate = findRevokedCertificate(
				issuerName, serialNumber);
		if (null == revokedCertificate) {
			LOG.debug("certificate valid: "
					+ childCertificate.getSubjectX500Principal());
			return new TrustLinkerResult(true);
		}
		if (revokedCertificate.getRevocationDate().after(validationDate)) {
			LOG.debug("CRL OK for: "
					+ childCertificate.getSubjectX500Principal() + " at "
					+ validationDate);
			return new TrustLinkerResult(true);
		}
		LOG.debug("certificate invalid: "
				+ childCertificate.getSubjectX500Principal());
		return new TrustLinkerResult(false,
				TrustLinkerResultReason.INVALID_REVOCATION_STATUS,
				"certificate revoked by cached CRL");
	}

	private String getCrlUrl(X509Certificate childCertificate) {

		URI crlUri = CrlTrustLinker.getCrlUri(childCertificate);
		if (null == crlUri) {
			LOG.warn("No CRL uri for: "
					+ childCertificate.getIssuerX500Principal().toString());
			return null;
		}
		try {
			return crlUri.toURL().toString();
		} catch (MalformedURLException e) {
			LOG.warn("malformed URL: " + e.getMessage(), e);
			return null;
		}
	}

	private void logAudit(String message) {

		try {
			InitialContext initialContext = new InitialContext();
			AuditDAO auditDAO = (AuditDAO) initialContext
					.lookup(AuditDAO.JNDI_BINDING);
			auditDAO.logAudit(message);
		} catch (NamingException e) {
			LOG.error("Failed to log audit message: " + message, e);
		}

	}

	@SuppressWarnings("unchecked")
	private RevokedCertificateEntity findRevokedCertificate(String issuer,
			BigInteger serialNumber) {

		Query query = this.entityManager
				.createNamedQuery(RevokedCertificateEntity.QUERY_WHERE_ISSUER_SERIAL);
		query.setParameter("issuer", issuer);
		query.setParameter("serialNumber", serialNumber);
		List<RevokedCertificateEntity> revokedCertificates = (List<RevokedCertificateEntity>) query
				.getResultList();
		if (revokedCertificates.isEmpty()) {
			return null;
		} else {
			return revokedCertificates.get(0);
		}
	}
}
