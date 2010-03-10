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
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.TransactionTimeout;

import be.fedict.trust.service.dao.CertificateAuthorityDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.RevokedCertificateEntity;
import be.fedict.trust.service.entity.TrustPointEntity;

/**
 * Certificate Authority DAO Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
public class CertificateAuthorityDAOBean implements CertificateAuthorityDAO {

	private static final Log LOG = LogFactory
			.getLog(CertificateAuthorityDAOBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	public CertificateAuthorityEntity findCertificateAuthority(String name) {

		LOG.debug("find CA: " + name);
		return this.entityManager.find(CertificateAuthorityEntity.class, name);
	}

	/**
	 * {@inheritDoc}
	 */
	public CertificateAuthorityEntity findCertificateAuthority(
			X509Certificate certificate) {

		LOG.debug("find CA: "
				+ certificate.getSubjectX500Principal().toString());
		return this.entityManager.find(CertificateAuthorityEntity.class,
				certificate.getSubjectX500Principal().toString());
	}

	/**
	 * {@inheritDoc}
	 */
	public CertificateAuthorityEntity addCertificateAuthority(
			X509Certificate certificate) {

		LOG.debug("add  CA: "
				+ certificate.getSubjectX500Principal().toString());
		CertificateAuthorityEntity certificateAuthority;
		try {
			certificateAuthority = new CertificateAuthorityEntity(null,
					certificate);
		} catch (CertificateEncodingException e) {
			LOG.error("Certificate encoding exception: " + e.getMessage());
			return null;
		}
		this.entityManager.persist(certificateAuthority);
		return certificateAuthority;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeCertificateAuthorities(TrustPointEntity trustPoint) {

		LOG.debug("remove CA's for trust point " + trustPoint.getName());
		Query query = this.entityManager
				.createNamedQuery(CertificateAuthorityEntity.DELETE_WHERE_TRUST_POINT);
		query.setParameter("trustPoint", trustPoint);
		int result = query.executeUpdate();
		LOG.debug("CA's removed: " + result);
	}

	/**
	 * {@inheritDoc}
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public RevokedCertificateEntity addRevokedCertificate(String issuerName,
			BigInteger serialNumber, Date revocationDate, BigInteger crlNumber) {

		RevokedCertificateEntity revokedCertificate = new RevokedCertificateEntity(
				issuerName, serialNumber, revocationDate, crlNumber);
		this.entityManager.persist(revokedCertificate);
		return revokedCertificate;
	}

	/**
	 * {@inheritDoc}
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@TransactionTimeout(1200)
	public void addRevokedCertificates(X509CRL crl, BigInteger crlNumber) {

		for (X509CRLEntry revokedCertificate : crl.getRevokedCertificates()) {
			X500Principal certificateIssuer = revokedCertificate
					.getCertificateIssuer();
			String issuerName;
			if (null == certificateIssuer) {
				issuerName = crl.getIssuerX500Principal().toString();
			} else {
				issuerName = certificateIssuer.toString();
			}
			BigInteger serialNumber = revokedCertificate.getSerialNumber();
			Date revocationDate = revokedCertificate.getRevocationDate();

			this.entityManager.persist(new RevokedCertificateEntity(issuerName,
					serialNumber, revocationDate, crlNumber));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public List<RevokedCertificateEntity> listRevokedCertificates(
			BigInteger crlNumber, String issuerName) {

		LOG.debug("list revoked certificates for crl number " + crlNumber
				+ " issuer " + issuerName);
		Query query = this.entityManager
				.createNamedQuery(RevokedCertificateEntity.QUERY_WHERE_ISSUER_CRL_NUMBER);
		query.setParameter("issuer", issuerName);
		query.setParameter("crlNumber", crlNumber);
		return (List<RevokedCertificateEntity>) query.getResultList();
	}

	/**
	 * {@inheritDoc}
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int removeOldRevokedCertificates(BigInteger crlNumber,
			String issuerName) {

		LOG.debug("deleting revoked certificates (issuer=" + issuerName
				+ " older then crl=" + crlNumber);

		Query query = this.entityManager
				.createNamedQuery(RevokedCertificateEntity.DELETE_WHERE_ISSUER_OLDER_CRL_NUMBER);
		query.setParameter("issuer", issuerName);
		query.setParameter("crlNumber", crlNumber);
		int deleteResult = query.executeUpdate();
		LOG.debug("delete result: " + deleteResult);
		this.entityManager.flush();
		return deleteResult;
	}
}
