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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;
import org.joda.time.DateTime;

import be.fedict.trust.CrlTrustLinker;
import be.fedict.trust.OnlineCrlRepository;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.RevokedCertificateEntity;
import be.fedict.trust.service.entity.RevokedCertificatePK;
import be.fedict.trust.service.entity.Status;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/trust/harvester") })
public class HarvesterMDB implements MessageListener {

	private static final Log LOG = LogFactory.getLog(HarvesterMDB.class);

	@PersistenceContext
	private EntityManager entityManager;

	@PostConstruct
	public void postConstructCallback() {
		LOG.debug("post construct");
	}

	@Resource
	private TimerService timerService;

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
		Date validationDate = new Date();
		X509CRL crl = onlineCrlRepository.findCrl(crlUri, validationDate);
		X509Certificate issuerCertificate;
		try {
			issuerCertificate = certificateAuthority.getCertificate();
		} catch (CertificateException e) {
			LOG.error("certificate error: " + e.getMessage(), e);
			return;
		}
		LOG.debug("checking integrity CRL...");
		boolean crlValidity = CrlTrustLinker.checkCrlIntegrity(crl,
				issuerCertificate, validationDate);
		if (false == crlValidity) {
			LOG.error("CRL invalid");
			return;
		}
		LOG.debug("processing CRL... " + caName);
		BigInteger crlNumber = getCrlNumber(crl);
		LOG.debug("CRL number: " + crlNumber);
		Set<? extends X509CRLEntry> revokedCertificates = crl
				.getRevokedCertificates();
		if (null != revokedCertificates) {
			for (X509CRLEntry revokedCertificate : revokedCertificates) {
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
				RevokedCertificateEntity revokedCertificateEntity = this.entityManager
						.find(RevokedCertificateEntity.class,
								new RevokedCertificatePK(issuerName,
										serialNumber));
				if (null == revokedCertificateEntity) {
					revokedCertificateEntity = new RevokedCertificateEntity(
							issuerName, serialNumber, revocationDate, crlNumber);
					this.entityManager.persist(revokedCertificateEntity);
				} else {
					revokedCertificateEntity.setCrlNumber(crlNumber);
				}
			}
		}
		if (null != crlNumber) {
			LOG.debug("deleting old CRL entries from the cache");
			Query deleteQuery = this.entityManager
					.createQuery("DELETE FROM RevokedCertificateEntity cert WHERE cert.crlNumber < :crlNumber AND cert.pk.issuer = :issuerName");
			deleteQuery.setParameter("crlNumber", crlNumber);
			deleteQuery.setParameter("issuerName", crl.getIssuerX500Principal()
					.toString());
			int deleteResult = deleteQuery.executeUpdate();
			LOG.debug("delete result: " + deleteResult);
		}

		LOG.debug("CRL this update: " + crl.getThisUpdate());
		LOG.debug("CRL next update: " + crl.getNextUpdate());
		DateTime timerExpiration = new DateTime(crl.getNextUpdate());
		// TODO fine-tune timer expiration
		LOG.debug("programming scheduler to: " + timerExpiration);
		this.timerService.createTimer(timerExpiration.toDate(), caName);

		certificateAuthority.setStatus(Status.ACTIVE);
		certificateAuthority.setThisUpdate(crl.getThisUpdate());
		certificateAuthority.setNextUpdate(crl.getNextUpdate());
		LOG.debug("cache activated for CA: " + crl.getIssuerX500Principal());
	}

	@Timeout
	public void scheduler(Timer timer) {
		String caName = (String) timer.getInfo();
		LOG.debug("scheduler timeout for CA: " + caName);
		// TODO implement me
	}

	private BigInteger getCrlNumber(X509CRL crl) {
		byte[] crlNumberExtensionValue = crl.getExtensionValue("2.5.29.20");
		if (null == crlNumberExtensionValue) {
			return null;
		}
		try {
			DEROctetString octetString = (DEROctetString) (new ASN1InputStream(
					new ByteArrayInputStream(crlNumberExtensionValue))
					.readObject());
			byte[] octets = octetString.getOctets();
			DERInteger integer = (DERInteger) new ASN1InputStream(octets)
					.readObject();
			BigInteger crlNumber = integer.getPositiveValue();
			return crlNumber;
		} catch (IOException e) {
			throw new RuntimeException("IO error: " + e.getMessage(), e);
		}
	}
}
