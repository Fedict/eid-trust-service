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

package be.fedict.trust.service.bean;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.TrustLinker;
import be.fedict.trust.crl.CrlTrustLinker;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.RevokedCertificateEntity;
import be.fedict.trust.service.entity.RevokedCertificatePK;
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

	public Boolean hasTrustLink(X509Certificate childCertificate,
			X509Certificate certificate, Date validationDate) {
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
			try {
				certificateAuthority = new CertificateAuthorityEntity(
						issuerName, crlUrl, certificate);
			} catch (CertificateEncodingException e) {
				LOG.error("certificate encoding error: " + e.getMessage(), e);
				return null;
			}
			this.entityManager.persist(certificateAuthority);
			try {
				notifyHarvester(issuerName);
			} catch (JMSException e) {
				LOG.error("could not notify harvester: " + e.getMessage(), e);
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
		RevokedCertificateEntity revokedCertificate = this.entityManager.find(
				RevokedCertificateEntity.class, new RevokedCertificatePK(
						issuerName, serialNumber));
		if (null == revokedCertificate) {
			LOG.debug("certificate valid: "
					+ childCertificate.getSubjectX500Principal());
			return true;
		}
		if (revokedCertificate.getRevocationDate().after(validationDate)) {
			LOG.debug("CRL OK for: "
					+ childCertificate.getSubjectX500Principal() + " at "
					+ validationDate);
			return true;
		}
		LOG.debug("certificate invalid: "
				+ childCertificate.getSubjectX500Principal());
		return false;
	}

	private void notifyHarvester(String issuerName) throws JMSException {
		QueueConnection queueConnection = this.queueConnectionFactory
				.createQueueConnection();
		try {
			QueueSession queueSession = queueConnection.createQueueSession(
					true, Session.AUTO_ACKNOWLEDGE);
			try {
				TextMessage textMessage = queueSession
						.createTextMessage(issuerName);
				QueueSender queueSender = queueSession.createSender(this.queue);
				try {
					queueSender.send(textMessage);
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
