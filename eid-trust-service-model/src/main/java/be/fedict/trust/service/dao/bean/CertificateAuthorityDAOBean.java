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

package be.fedict.trust.service.dao.bean;

import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.dao.CertificateAuthorityDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.RevokedCertificateEntity;
import be.fedict.trust.service.entity.RevokedCertificatePK;
import be.fedict.trust.service.entity.TrustPointEntity;

/**
 * Certificate Authority DAO Bean implementation.
 * 
 * @author wvdhaute
 */
@Stateless
public class CertificateAuthorityDAOBean implements CertificateAuthorityDAO {

	private static final Log LOG = LogFactory
			.getLog(CertificateAuthorityDAOBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	public CertificateAuthorityEntity findCertificateAuthority(String name) {
		LOG.debug("find CA: " + name);
		return this.entityManager.find(CertificateAuthorityEntity.class, name);
	}

	public CertificateAuthorityEntity findCertificateAuthority(
			X509Certificate certificate) {
		LOG.debug("find CA: "
				+ certificate.getSubjectX500Principal().toString());
		return this.entityManager.find(CertificateAuthorityEntity.class,
				certificate.getSubjectX500Principal().toString());
	}

	public CertificateAuthorityEntity addCertificateAuthority(
			X509Certificate certificate, String crlUrl) {
		LOG.debug("add  CA: "
				+ certificate.getSubjectX500Principal().toString());
		CertificateAuthorityEntity certificateAuthority;
		try {
			certificateAuthority = new CertificateAuthorityEntity(crlUrl,
					certificate);
		} catch (CertificateEncodingException e) {
			LOG.error("Certificate encoding exception: " + e.getMessage());
			return null;
		}
		this.entityManager.persist(certificateAuthority);
		return certificateAuthority;
	}

	public void removeCertificateAuthorities(TrustPointEntity trustPoint) {
		LOG.debug("remove CA's for trust point " + trustPoint.getName());
		Query query = this.entityManager
				.createNamedQuery(CertificateAuthorityEntity.DELETE_WHERE_TRUST_POINT);
		query.setParameter("trustPoint", trustPoint);
		int result = query.executeUpdate();
		LOG.debug("CA's removed: " + result);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public RevokedCertificateEntity addRevokedCertificate(String issuerName,
			BigInteger serialNumber, Date revocationDate, BigInteger crlNumber) {
		RevokedCertificateEntity revokedCertificate = new RevokedCertificateEntity(
				issuerName, serialNumber, revocationDate, crlNumber);
		this.entityManager.persist(revokedCertificate);
		return revokedCertificate;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void updateRevokedCertificates(
			Set<X509CRLEntry> revokedCertificates, BigInteger crlNumber,
			X500Principal crlIssuer) {
		LOG.debug("Update " + revokedCertificates.size()
				+ " revoked certificates (crlNumber=" + crlNumber + ")");
		for (X509CRLEntry revokedCertificate : revokedCertificates) {
			X500Principal certificateIssuer = revokedCertificate
					.getCertificateIssuer();
			String issuerName;
			if (null == certificateIssuer) {
				issuerName = crlIssuer.toString();
			} else {
				issuerName = certificateIssuer.toString();
			}
			BigInteger serialNumber = revokedCertificate.getSerialNumber();
			Date revocationDate = revokedCertificate.getRevocationDate();

			// lookup
			RevokedCertificateEntity revokedCertificateEntity = this.entityManager
					.find(RevokedCertificateEntity.class,
							new RevokedCertificatePK(issuerName, serialNumber
									.toString()));

			if (null != revokedCertificateEntity) {
				// already exists, update revocationDate and crl number
				revokedCertificateEntity.setRevocationDate(revocationDate);
				revokedCertificateEntity.setCrlNumber(crlNumber);
			} else {
				// don't exist yet, add
				this.entityManager.persist(new RevokedCertificateEntity(
						issuerName, serialNumber, revocationDate, crlNumber));
			}
		}
	}

	// @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void updateRevokedCertificates(
			Set<X509CRLEntry> revokedCertificates, BigInteger crlNumber,
			X500Principal crlIssuer,
			Map<String, RevokedCertificateEntity> revokedCertificatesMap) {
		LOG.debug("Update " + revokedCertificates.size()
				+ " revoked certificates (crlNumber=" + crlNumber + ")");
		for (X509CRLEntry revokedCertificate : revokedCertificates) {
			X500Principal certificateIssuer = revokedCertificate
					.getCertificateIssuer();
			String issuerName;
			if (null == certificateIssuer) {
				issuerName = crlIssuer.toString();
			} else {
				issuerName = certificateIssuer.toString();
			}
			BigInteger serialNumber = revokedCertificate.getSerialNumber();
			Date revocationDate = revokedCertificate.getRevocationDate();

			// lookup
			RevokedCertificateEntity revokedCertificateEntity = revokedCertificatesMap
					.get(serialNumber.toString());

			if (null != revokedCertificateEntity) {
				// already exists, update revocationDate and crl number
				revokedCertificateEntity.setRevocationDate(revocationDate);
				revokedCertificateEntity.setCrlNumber(crlNumber);
			} else {
				// don't exist yet, add
				this.entityManager.persist(new RevokedCertificateEntity(
						issuerName, serialNumber, revocationDate, crlNumber));
			}
		}
	}

	// @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public int removeOldRevokedCertificates(BigInteger crlNumber,
			String issuerName) {
		LOG.debug("deleting revoked certificates (issuer=" + issuerName
				+ " older than crl=" + crlNumber + ")");
		Query query = this.entityManager
				.createNamedQuery(RevokedCertificateEntity.DELETE_WHERE_ISSUER_OLDER_CRL_NUMBER);
		query.setParameter("issuer", issuerName);
		query.setParameter("crlNumber", crlNumber);
		int deleteResult = query.executeUpdate();
		LOG.debug("delete result: " + deleteResult);
		this.entityManager.flush();
		return deleteResult;
	}

	public int removeRevokedCertificates(String issuerName) {
		LOG.debug("deleting revoked certificates (issuer=" + issuerName + ")");

		Query query = this.entityManager
				.createNamedQuery(RevokedCertificateEntity.DELETE_WHERE_ISSUER);
		query.setParameter("issuer", issuerName);
		int deleteResult = query.executeUpdate();
		LOG.debug("delete result: " + deleteResult);
		return deleteResult;
	}

	public BigInteger findCrlNumber(String issuerName) {
		LOG.debug("get CRL number for " + issuerName);
		Query query = this.entityManager
				.createNamedQuery(RevokedCertificateEntity.QUERY_CRL_NUMBER_WHERE_ISSUER);
		query.setParameter("issuer", issuerName);
		try {
			return (BigInteger) query.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<CertificateAuthorityEntity> listCertificateAuthorities() {
		Query query = this.entityManager
				.createNamedQuery(CertificateAuthorityEntity.QUERY_ALL);
		return (List<CertificateAuthorityEntity>) query.getResultList();
	}

	public long getTotalCachedCertificates() {
		/*
		 * Oracle has problems executing this query. So we no longer use it
		 * within the admin portal.
		 */
		Query query = this.entityManager
				.createNamedQuery(RevokedCertificateEntity.QUERY_COUNT_ALL);
		return (Long) query.getSingleResult();
	}

	public long getTotalActiveCachedCAs() {
		Query query = this.entityManager
				.createNamedQuery(CertificateAuthorityEntity.QUERY_COUNT_ACTIVE);
		return (Long) query.getSingleResult();
	}

	@SuppressWarnings("unchecked")
	public List<CertificateAuthorityEntity> listActiveCertificateAuthorities() {
		Query query = this.entityManager
				.createNamedQuery(CertificateAuthorityEntity.QUERY_ACTIVE);
		return (List<CertificateAuthorityEntity>) query.getResultList();
	}

	public void removeCertificateAuthority(CertificateAuthorityEntity ca) {
		String caName = ca.getName();
		CertificateAuthorityEntity caEntity = this.entityManager.find(
				CertificateAuthorityEntity.class, caName);
		LOG.debug("removing CA entity: " + caName);
		this.entityManager.remove(caEntity);
	}

	@SuppressWarnings("unchecked")
	public List<RevokedCertificateEntity> getRevokedCertificates(
			String issuerName) {
		LOG.debug("get revoked certificates from database for CA: "
				+ issuerName);
		Query query = this.entityManager
				.createNamedQuery(RevokedCertificateEntity.QUERY_WHERE_ISSUER);
		query.setParameter("issuer", issuerName);
		return query.getResultList();
	}
}
