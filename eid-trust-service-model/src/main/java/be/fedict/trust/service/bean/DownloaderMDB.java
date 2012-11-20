/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2012 FedICT.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.interceptor.Interceptors;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.openssl.PEMReader;

import be.fedict.trust.NetworkConfig;
import be.fedict.trust.service.NotificationService;
import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.SnmpConstants;
import be.fedict.trust.service.dao.AuditDAO;
import be.fedict.trust.service.dao.CertificateAuthorityDAO;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.Status;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.snmp.SNMP;
import be.fedict.trust.service.snmp.SNMPInterceptor;

/**
 * Downloader Message Driven Bean.
 * 
 * @author Frank Cornelis
 * 
 */
@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = DownloaderMDB.DOWNLOADER_QUEUE_LOCATION) })
@Interceptors(SNMPInterceptor.class)
public class DownloaderMDB implements MessageListener {

	private static final Log LOG = LogFactory.getLog(DownloaderMDB.class);

	public static final String DOWNLOADER_QUEUE_NAME = "TrustServiceDownloader";
	public static final String DOWNLOADER_QUEUE_LOCATION = "queue/trust/downloader";

	@EJB
	private CertificateAuthorityDAO certificateAuthorityDAO;

	@EJB
	private ConfigurationDAO configurationDAO;

	@SNMP(oid = SnmpConstants.CRL_DOWNLOAD_FAILURES)
	private Long failures = 0L;

	@EJB
	private AuditDAO auditDAO;

	@EJB
	private NotificationService notificationService;

	@EJB
	private SchedulingService schedulingService;

	public void onMessage(Message message) {
		LOG.debug("onMessage");
		DownloadMessage downloadMessage = null;
		ColdStartMessage coldStartMessage = null;
		try {
			String messageType = message
					.getStringProperty(JMSMessage.MESSAGE_TYPE_PROPERTY);
			if (messageType.equals(DownloadMessage.class.getSimpleName())) {
				downloadMessage = new DownloadMessage(message);
			} else if (messageType.equals(ColdStartMessage.class
					.getSimpleName())) {
				coldStartMessage = new ColdStartMessage(message);
			}
		} catch (JMSException e) {
			LOG.error("JMS error: " + e.getMessage(), e);
			return;
		}
		processDownloadMessage(downloadMessage);
		processColdStartMessage(coldStartMessage);

	}

	private void processColdStartMessage(ColdStartMessage coldStartMessage) {
		if (null == coldStartMessage) {
			return;
		}

		String crlUrl = coldStartMessage.getCrlUrl();
		String certUrl = coldStartMessage.getCertUrl();
		LOG.debug("cold start CRL URL: " + crlUrl);
		LOG.debug("cold start CA URL: " + certUrl);

		File crlFile = download(crlUrl);
		File certFile = download(certUrl);

		// parsing
		CertificateFactory certificateFactory;
		try {
			certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			LOG.debug("certificate factory error: " + e.getMessage(), e);
			crlFile.delete();
			certFile.delete();
			return;
		}

		X509Certificate certificate = null;
		try {
			certificate = (X509Certificate) certificateFactory
					.generateCertificate(new FileInputStream(certFile));
		} catch (Exception e) {
			LOG.debug("error DER-parsing certificate");
			try {
				PEMReader pemReader = new PEMReader(new FileReader(certFile));
				certificate = (X509Certificate) pemReader.readObject();
				pemReader.close();
			} catch (Exception e2) {
				retry("error PEM-parsing certificate", e, certFile, crlFile);
			}
		}
		certFile.delete();

		X509CRL crl = null;
		try {
			crl = (X509CRL) certificateFactory.generateCRL(new FileInputStream(
					crlFile));
		} catch (Exception e) {
			retry("error parsing CRL", e, crlFile);
		}

		// first check whether the two correspond
		try {
			crl.verify(certificate.getPublicKey());
		} catch (Exception e) {
			LOG.error("no correspondence between CRL and CA");
			LOG.error("CRL issuer: " + crl.getIssuerX500Principal());
			LOG.debug("CA subject: " + certificate.getSubjectX500Principal());
			crlFile.delete();
			return;
		}
		LOG.debug("CRL matches CA: " + certificate.getSubjectX500Principal());

		// skip expired CAs
		Date now = new Date();
		Date notAfter = certificate.getNotAfter();
		if (now.after(notAfter)) {
			LOG.warn("CA already expired: "
					+ certificate.getSubjectX500Principal());
			crlFile.delete();
			return;
		}

		// create database entitities
		CertificateAuthorityEntity certificateAuthority = this.certificateAuthorityDAO
				.findCertificateAuthority(certificate);
		if (null != certificateAuthority) {
			LOG.debug("CA already in cache: "
					+ certificate.getSubjectX500Principal());
			crlFile.delete();
			return;
		}

		/*
		 * Lookup Root CA's trust point via parent certificates' CA entity.
		 */
		String parentIssuerName = certificate.getIssuerX500Principal()
				.toString();
		CertificateAuthorityEntity parentCertificateAuthority = this.certificateAuthorityDAO
				.findCertificateAuthority(parentIssuerName);
		if (null == parentCertificateAuthority) {
			LOG.error("CA not found for " + parentIssuerName + " ?!");
			crlFile.delete();
			return;
		}
		LOG.debug("parent CA: " + parentCertificateAuthority.getName());
		TrustPointEntity parentTrustPoint = parentCertificateAuthority
				.getTrustPoint();
		if (null != parentTrustPoint) {
			LOG.debug("trust point parent: " + parentTrustPoint.getName());
			LOG.debug("previous trust point fire data: "
					+ parentTrustPoint.getFireDate());
		} else {
			LOG.debug("no parent trust point");
		}

		// create new CA
		certificateAuthority = this.certificateAuthorityDAO
				.addCertificateAuthority(certificate, crlUrl);

		// prepare harvesting
		certificateAuthority.setTrustPoint(parentTrustPoint);
		certificateAuthority.setStatus(Status.PROCESSING);
		if (null != certificateAuthority.getTrustPoint()
				&& null == certificateAuthority.getTrustPoint().getFireDate()) {
			try {
				this.schedulingService.startTimer(certificateAuthority
						.getTrustPoint());
			} catch (InvalidCronExpressionException e) {
				LOG.error("invalid cron expression");
				crlFile.delete();
				return;
			}
		}

		// notify harvester
		String crlFilePath = crlFile.getAbsolutePath();
		try {
			this.notificationService.notifyHarvester(certificate
					.getSubjectX500Principal().toString(), crlFilePath, false);
		} catch (JMSException e) {
			crlFile.delete();
			throw new RuntimeException(e);
		}
	}

	private void retry(String errorMessage, Exception e, File... files) {
		LOG.error(errorMessage);
		if (null != e) {
			LOG.error(e.getMessage(), e);
		}
		this.auditDAO.logAudit(errorMessage);
		this.failures++;
		for (File file : files) {
			file.delete();
		}
		throw new RuntimeException();
	}

	private void processDownloadMessage(DownloadMessage downloadMessage) {
		if (null == downloadMessage) {
			return;
		}

		String caName = downloadMessage.getCaName();
		boolean update = downloadMessage.isUpdate();

		LOG.debug("issuer: " + caName);
		CertificateAuthorityEntity certificateAuthority = this.certificateAuthorityDAO
				.findCertificateAuthority(caName);
		if (null == certificateAuthority) {
			LOG.error("unknown certificate authority: " + caName);
			return;
		}
		if (!update && Status.PROCESSING != certificateAuthority.getStatus()) {
			/*
			 * Possible that another harvester instance already activated or is
			 * processing the CA cache in the meanwhile.
			 */
			LOG.debug("CA status not marked for processing");
			return;
		}

		String crlUrl = certificateAuthority.getCrlUrl();
		if (null == crlUrl) {
			LOG.warn("No CRL url for CA " + certificateAuthority.getName());
			certificateAuthority.setStatus(Status.NONE);
			return;
		}

		File crlFile = download(crlUrl);
		String crlFilePath = crlFile.getAbsolutePath();

		try {
			this.notificationService.notifyHarvester(caName, crlFilePath,
					update);
		} catch (JMSException e) {
			crlFile.delete();
			throw new RuntimeException(e);
		}
	}

	private File download(String url) {
		NetworkConfig networkConfig = this.configurationDAO.getNetworkConfig();
		HttpClient httpClient = new HttpClient();
		if (null != networkConfig) {
			httpClient.getHostConfiguration().setProxy(
					networkConfig.getProxyHost(), networkConfig.getProxyPort());
		}
		HttpClientParams httpClientParams = httpClient.getParams();
		httpClientParams.setParameter("http.socket.timeout", new Integer(
				1000 * 20));
		LOG.debug("downloading: " + url);
		GetMethod getMethod = new GetMethod(url);
		getMethod.addRequestHeader("User-Agent", "eID Trust Service Client");
		int statusCode;
		try {
			statusCode = httpClient.executeMethod(getMethod);
		} catch (Exception e) {
			downloadFailed(url);
			throw new RuntimeException();
		}
		if (HttpURLConnection.HTTP_OK != statusCode) {
			LOG.debug("HTTP status code: " + statusCode);
			downloadFailed(url);
			throw new RuntimeException();
		}

		String downloadFilePath;
		File downloadFile = null;
		try {
			downloadFile = File.createTempFile("trust-service-", ".der");
			InputStream downloadInputStream = getMethod
					.getResponseBodyAsStream();
			OutputStream downloadOutputStream = new FileOutputStream(
					downloadFile);
			IOUtils.copy(downloadInputStream, downloadOutputStream);
			IOUtils.closeQuietly(downloadInputStream);
			IOUtils.closeQuietly(downloadOutputStream);
			downloadFilePath = downloadFile.getAbsolutePath();
			LOG.debug("temp file: " + downloadFilePath);
		} catch (IOException e) {
			downloadFailed(url);
			if (null != downloadFile) {
				downloadFile.delete();
			}
			throw new RuntimeException(e);
		}
		return downloadFile;
	}

	private void downloadFailed(String url) {
		this.auditDAO.logAudit("Failed to download from: " + url);
		this.failures++;
	}
}
