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

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.RevocationData;
import be.fedict.trust.TrustLinker;
import be.fedict.trust.TrustLinkerResult;
import be.fedict.trust.crl.CrlTrustLinker;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.RevokedCertificateEntity;
import be.fedict.trust.service.entity.Status;

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

	private final QueueConnectionFactory queueConnectionFactory;

	private final Queue queue;

	public TrustServiceTrustLinker(EntityManager entityManager,
			QueueConnectionFactory queueConnectionFactory, Queue queue) {
		this.entityManager = entityManager;
		this.queueConnectionFactory = queueConnectionFactory;
		this.queue = queue;
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
			URI crlUri = CrlTrustLinker.getCrlUri(childCertificate);
			String crlUrl;
			try {
				crlUrl = crlUri.toURL().toString();
			} catch (MalformedURLException e) {
				LOG.warn("malformed URL: " + e.getMessage(), e);
				return null;
			}

			/*
			 * Lookup Root CA's trust point via parent certificates' CA entity.
			 */
			String parentIssuerName = certificate.getIssuerX500Principal()
					.toString();
			CertificateAuthorityEntity parentCertificateAuthority = this.entityManager
					.find(CertificateAuthorityEntity.class, parentIssuerName);
			if (null == parentCertificateAuthority) {
				LOG.error("CA not found for " + parentIssuerName + " ?!");
				// XXX: audit?
				return null;
			}

			// create new CA
			try {
				certificateAuthority = new CertificateAuthorityEntity(crlUrl,
						certificate);
				certificateAuthority.setTrustPoint(parentCertificateAuthority
						.getTrustPoint());
			} catch (CertificateEncodingException e) {
				LOG.error("certificate encoding error: " + e.getMessage(), e);
				return null;
			}
			this.entityManager.persist(certificateAuthority);

			// notify harvester
			try {
				notifyHarvester(issuerName);
			} catch (JMSException e) {
				LOG.error("could not notify harvester: " + e.getMessage(), e);
				// XXX: audit
			}
			LOG.debug("harvester notified.");
			return null;
		}
		if (Status.ACTIVE != certificateAuthority.getStatus()) {
			LOG.debug("CA revocation data cache not yet active: " + issuerName);
			/*
			 * Harvester is still busy processing the first CRL.
			 */
			return null;
		}
		/*
		 * Let's use the cached revocation data
		 */
		Date thisUpdate = certificateAuthority.getThisUpdate();
		if (null == thisUpdate) {
			LOG.warn("no thisUpdate value");
			return null;
		}
		Date nextUpdate = certificateAuthority.getNextUpdate();
		if (null == nextUpdate) {
			LOG.warn("no nextUpdate value");
			return null;
		}
		/*
		 * First check whether the cached revocation data is up-to-date.
		 */
		if (thisUpdate.after(validationDate)) {
			LOG.warn("cached CRL data too recent");
			return null;
		}
		if (validationDate.after(nextUpdate)) {
			LOG.warn("cached CRL data too old");
			return null;
		}
		LOG.debug("using cached CRL data");
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
		return new TrustLinkerResult(true);
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

	private void notifyHarvester(String issuerName) throws JMSException {
		QueueConnection queueConnection = this.queueConnectionFactory
				.createQueueConnection();
		try {
			QueueSession queueSession = queueConnection.createQueueSession(
					true, Session.AUTO_ACKNOWLEDGE);
			try {
				HarvestMessage harvestMessage = new HarvestMessage(issuerName,
						false);
				QueueSender queueSender = queueSession.createSender(this.queue);
				try {
					queueSender
							.send(harvestMessage.getJMSMessage(queueSession));
				} finally {
					queueSender.close();
				}
			} finally {
				queueSession.close();
			}
		} finally {
			queueConnection.close();
		}
	}
}
