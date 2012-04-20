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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CRLException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.interceptor.Interceptors;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.IssuingDistributionPoint;
import org.bouncycastle.asn1.x509.TBSCertList;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.provider.X509CRLEntryObject;
import org.bouncycastle.x509.extension.X509ExtensionUtil;

import be.fedict.trust.crl.CrlTrustLinker;
import be.fedict.trust.service.SnmpConstants;
import be.fedict.trust.service.dao.AuditDAO;
import be.fedict.trust.service.dao.CertificateAuthorityDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.Status;
import be.fedict.trust.service.snmp.SNMP;
import be.fedict.trust.service.snmp.SNMPInterceptor;

/**
 * Harvester Message Driven Bean.
 * 
 * @author Frank Cornelis
 * 
 */
@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = HarvesterMDB.HARVESTER_QUEUE_LOCATION) })
@Interceptors(SNMPInterceptor.class)
public class HarvesterMDB implements MessageListener {

	private static final Log LOG = LogFactory.getLog(HarvesterMDB.class);

	public static final String HARVESTER_QUEUE_NAME = "TrustServiceHarvester";
	public static final String HARVESTER_QUEUE_LOCATION = "queue/trust/harvester";

	private static final int BATCH_SIZE = 500;

	@EJB
	private CertificateAuthorityDAO certificateAuthorityDAO;

	@EJB
	private AuditDAO auditDAO;

	@SNMP(oid = SnmpConstants.CRL_DOWNLOAD_FAILURES)
	private Long failures = 0L;

	@PostConstruct
	public void postConstructCallback() {
		LOG.debug("post construct");
	}

	@SNMP(oid = SnmpConstants.CACHE_REFRESH)
	public void onMessage(Message message) {
		LOG.debug("onMessage");
		HarvestMessage harvestMessage;
		try {
			harvestMessage = new HarvestMessage(message);
		} catch (JMSException e) {
			LOG.error("JMS error: " + e.getMessage(), e);
			return;
		}
		String caName = harvestMessage.getCaName();
		boolean update = harvestMessage.isUpdate();
		String crlFilePath = harvestMessage.getCrlFile();
		File crlFile = new File(crlFilePath);

		LOG.debug("issuer: " + caName);
		CertificateAuthorityEntity certificateAuthority = this.certificateAuthorityDAO
				.findCertificateAuthority(caName);
		if (null == certificateAuthority) {
			LOG.error("unknown certificate authority: " + caName);
			deleteCrlFile(crlFile);
			return;
		}
		if (!update && Status.PROCESSING != certificateAuthority.getStatus()) {
			/*
			 * Possible that another harvester instance already activated or is
			 * processing the CA cache in the meanwhile.
			 */
			LOG.debug("CA status not marked for processing");
			deleteCrlFile(crlFile);
			return;
		}

		FileInputStream crlInputStream;
		try {
			crlInputStream = new FileInputStream(crlFile);
		} catch (FileNotFoundException e) {
			LOG.error("CRL file does not exist: " + crlFilePath);
			return;
		}
		X509CRL crl;
		try {
			CertificateFactory certificateFactory = CertificateFactory
					.getInstance("X.509", "BC");
			crl = (X509CRL) certificateFactory.generateCRL(crlInputStream);
		} catch (Exception e) {
			LOG.error("BC error: " + e.getMessage(), e);
			deleteCrlFile(crlFile);
			return;
		}

		Date validationDate = new Date();

		X509Certificate issuerCertificate = certificateAuthority
				.getCertificate();

		LOG.debug("checking integrity CRL...");
		boolean crlValid = CrlTrustLinker.checkCrlIntegrity(crl,
				issuerCertificate, validationDate);
		if (!crlValid) {
			this.auditDAO.logAudit("Invalid CRL for CA=" + caName);
			deleteCrlFile(crlFile);
			return;
		}
		BigInteger crlNumber = getCrlNumber(crl);
		LOG.debug("CRL number: " + crlNumber);

		BigInteger currentCrlNumber = this.certificateAuthorityDAO
				.findCrlNumber(caName);
		if (null != currentCrlNumber
				&& currentCrlNumber.compareTo(crlNumber) >= 0
				&& certificateAuthority.getStatus() == Status.ACTIVE) {
			// current CRL cache is higher or equal, no update needed
			LOG.debug("current CA cache is new enough.");
			deleteCrlFile(crlFile);
			return;
		}

		LOG.debug("processing CRL... " + caName);
		boolean isIndirect;
		Enumeration revokedCertificatesEnum;
		try {
			isIndirect = isIndirectCRL(crl);
			revokedCertificatesEnum = getRevokedCertificatesEnum(crl);
		} catch (Exception e) {
			this.auditDAO.logAudit("Failed to parse CRL for CA=" + caName);
			this.failures++;
			throw new RuntimeException(e);
		}

		int entries = 0;
		if (revokedCertificatesEnum.hasMoreElements()) {
			/*
			 * Split up persisting the crl entries to avoid memory issues.
			 */
			Set<X509CRLEntry> revokedCertsBatch = new HashSet<X509CRLEntry>();
			X500Principal previousCertificateIssuer = crl
					.getIssuerX500Principal();
			int added = 0;
			while (revokedCertificatesEnum.hasMoreElements()) {

				TBSCertList.CRLEntry entry = (TBSCertList.CRLEntry) revokedCertificatesEnum
						.nextElement();
				X509CRLEntryObject revokedCertificate = new X509CRLEntryObject(
						entry, isIndirect, previousCertificateIssuer);
				previousCertificateIssuer = revokedCertificate
						.getCertificateIssuer();

				revokedCertsBatch.add(revokedCertificate);
				added++;
				if (added == BATCH_SIZE) {
					/*
					 * Persist batch
					 */
					this.certificateAuthorityDAO.updateRevokedCertificates(
							revokedCertsBatch, crlNumber,
							crl.getIssuerX500Principal());
					entries += revokedCertsBatch.size();
					revokedCertsBatch.clear();
					added = 0;
				}
			}
			/*
			 * Persist final batch
			 */
			this.certificateAuthorityDAO.updateRevokedCertificates(
					revokedCertsBatch, crlNumber, crl.getIssuerX500Principal());
			entries += revokedCertsBatch.size();

			/*
			 * Cleanup redundant CRL entries
			 */
			if (null != crlNumber) {
				this.certificateAuthorityDAO.removeOldRevokedCertificates(
						crlNumber, crl.getIssuerX500Principal().toString());
			}
		}

		deleteCrlFile(crlFile);

		LOG.debug("CRL this update: " + crl.getThisUpdate());
		LOG.debug("CRL next update: " + crl.getNextUpdate());
		certificateAuthority.setStatus(Status.ACTIVE);
		certificateAuthority.setThisUpdate(crl.getThisUpdate());
		certificateAuthority.setNextUpdate(crl.getNextUpdate());
		LOG.debug("cache activated for CA: " + crl.getIssuerX500Principal()
				+ " (entries=" + entries + ")");
	}

	private void deleteCrlFile(File crlFile) {
		boolean deletedCrlFile = crlFile.delete();
		if (!deletedCrlFile) {
			LOG.warn("could not delete temp CRL file: "
					+ crlFile.getAbsolutePath());
		}
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
			return integer.getPositiveValue();
		} catch (IOException e) {
			throw new RuntimeException("IO error: " + e.getMessage(), e);
		}
	}

	/**
	 * Added for as {@link X509CRL#getRevokedCertificates()} is memory intensive
	 * because it is returning a complete set, for huge CRL's this can get kinda
	 * out of hand. So we return an enumeration of the DER
	 * {@link TBSCertList.CRLEntry} objects.
	 * 
	 * @param crl
	 *            the CRL
	 * @return {@link Enumeration} of {@link TBSCertList.CRLEntry}'s.
	 * @throws IOException
	 *             something went wrong parsing.
	 * @throws CRLException
	 *             something went wrong parsing.
	 */
	@SuppressWarnings("unchecked")
	private Enumeration<TBSCertList.CRLEntry> getRevokedCertificatesEnum(
			X509CRL crl) throws IOException, CRLException {
		byte[] certList = crl.getTBSCertList();
		ByteArrayInputStream bais = new ByteArrayInputStream(certList);
		ASN1InputStream aIn = new ASN1InputStream(bais, Integer.MAX_VALUE, true);
		ASN1Sequence seq = (ASN1Sequence) aIn.readObject();
		TBSCertList cl = TBSCertList.getInstance(seq);
		return cl.getRevokedCertificateEnumeration();
	}

	/**
	 * Returns if the specified CRL is indirect.
	 * 
	 * @param crl
	 *            the CRL
	 * @return true or false
	 * @throws CRLException
	 *             something went wrong reading the
	 *             {@link org.bouncycastle.asn1.x509.IssuingDistributionPoint}.
	 */
	private boolean isIndirectCRL(X509CRL crl) throws CRLException {
		byte[] idp = crl
				.getExtensionValue(X509Extensions.IssuingDistributionPoint
						.getId());
		boolean isIndirect = false;
		try {
			if (idp != null) {
				isIndirect = IssuingDistributionPoint.getInstance(
						X509ExtensionUtil.fromExtensionValue(idp))
						.isIndirectCRL();
			}
		} catch (Exception e) {
			throw new CRLException(
					"Exception reading IssuingDistributionPoint", e);
		}

		return isIndirect;
	}
}
