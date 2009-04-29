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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509CRL;
import java.util.Date;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.OnlineCrlRepository;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.Status;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/trust/harvester") })
public class HarvesterMDB implements MessageListener {

	private static final Log LOG = LogFactory.getLog(HarvesterMDB.class);

	@PersistenceContext
	private EntityManager entityManager;

	public void onMessage(Message message) {
		LOG.debug("onMessage");
		TextMessage textMessage = (TextMessage) message;
		String caName;
		try {
			caName = textMessage.getText();
		} catch (JMSException e) {
			LOG.error("JMS error: " + e.getMessage());
			return;
		}
		LOG.debug("issuer: " + caName);
		CertificateAuthorityEntity certificateAuthority = this.entityManager
				.find(CertificateAuthorityEntity.class, caName);
		if (null == certificateAuthority) {
			LOG.warn("unknown certificate authority: " + caName);
			return;
		}
		if (Status.INACTIVE != certificateAuthority.getStatus()) {
			/*
			 * Possible that another harvester instance already activated the CA
			 * cache in the meanwhile.
			 */
			LOG.debug("CA status not inactive");
			return;
		}
		String crlUrl = certificateAuthority.getCrlUrl();
		OnlineCrlRepository onlineCrlRepository = new OnlineCrlRepository(
				TrustServiceBean.NETWORK_CONFIG);
		URI crlUri;
		try {
			crlUri = new URI(crlUrl);
		} catch (URISyntaxException e) {
			LOG.error("CRL URI error: " + e.getMessage(), e);
			return;
		}
		X509CRL crl = onlineCrlRepository.findCrl(crlUri, new Date());
		// TODO: process CRL
	}
}
