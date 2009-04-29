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

import java.net.MalformedURLException;
import java.net.URI;
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

import be.fedict.trust.CrlTrustLinker;
import be.fedict.trust.TrustLinker;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
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
			certificateAuthority = new CertificateAuthorityEntity(issuerName,
					crlUrl);
			this.entityManager.persist(certificateAuthority);
			try {
				notifyHarvester(issuerName);
			} catch (JMSException e) {
				LOG.error("could not notify harvester: " + e.getMessage(), e);
			}
			return null;
		}
		if (Status.ACTIVE != certificateAuthority.getStatus()) {
			LOG.debug("CA revocation data cache not yet active: " + issuerName);
			return null;
		}
		// TODO: use the cached revocation data
		return null;
	}

	private void notifyHarvester(String issuerName) throws JMSException {
		QueueConnection queueConnection = this.queueConnectionFactory
				.createQueueConnection();
		QueueSession queueSession = queueConnection.createQueueSession(false,
				Session.AUTO_ACKNOWLEDGE);
		try {
			TextMessage textMessage = queueSession
					.createTextMessage(issuerName);
			QueueSender queueSender = queueSession.createSender(this.queue);
			queueSender.send(textMessage);
		} finally {
			queueSession.close();
		}
	}
}
