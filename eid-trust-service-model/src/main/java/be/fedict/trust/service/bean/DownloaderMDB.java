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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

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

import be.fedict.trust.NetworkConfig;
import be.fedict.trust.service.NotificationService;
import be.fedict.trust.service.SnmpConstants;
import be.fedict.trust.service.dao.AuditDAO;
import be.fedict.trust.service.dao.CertificateAuthorityDAO;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.Status;
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

	public void onMessage(Message message) {
		LOG.debug("onMessage");
		DownloadMessage downloadMessage;
		try {
			downloadMessage = new DownloadMessage(message);
		} catch (JMSException e) {
			LOG.error("JMS error: " + e.getMessage(), e);
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

		NetworkConfig networkConfig = this.configurationDAO.getNetworkConfig();
		HttpClient httpClient = new HttpClient();
		if (null != networkConfig) {
			httpClient.getHostConfiguration().setProxy(
					networkConfig.getProxyHost(), networkConfig.getProxyPort());
		}
		HttpClientParams httpClientParams = httpClient.getParams();
		httpClientParams.setParameter("http.socket.timeout", new Integer(
				1000 * 20));
		LOG.debug("downloading CRL from: " + crlUrl);
		GetMethod getMethod = new GetMethod(crlUrl);
		getMethod.addRequestHeader("User-Agent", "jTrust CRL Client");
		int statusCode;
		try {
			statusCode = httpClient.executeMethod(getMethod);
		} catch (Exception e) {
			downloadFailed(caName, crlUrl);
			throw new RuntimeException();
		}
		if (HttpURLConnection.HTTP_OK != statusCode) {
			LOG.debug("HTTP status code: " + statusCode);
			downloadFailed(caName, crlUrl);
			throw new RuntimeException();
		}

		String crlFilePath;
		File crlFile = null;
		try {
			crlFile = File.createTempFile("crl-", ".der");
			InputStream crlInputStream = getMethod.getResponseBodyAsStream();
			OutputStream crlOutputStream = new FileOutputStream(crlFile);
			IOUtils.copy(crlInputStream, crlOutputStream);
			IOUtils.closeQuietly(crlInputStream);
			IOUtils.closeQuietly(crlOutputStream);
			crlFilePath = crlFile.getAbsolutePath();
			LOG.debug("temp CRL file: " + crlFilePath);
		} catch (IOException e) {
			downloadFailed(caName, crlUrl);
			if (null != crlFile) {
				crlFile.delete();
			}
			throw new RuntimeException(e);
		}
		try {
			this.notificationService.notifyHarvester(caName, crlFilePath,
					update);
		} catch (JMSException e) {
			crlFile.delete();
			throw new RuntimeException(e);
		}
	}

	private void downloadFailed(String caName, String crlUrl) {
		this.auditDAO.logAudit("Failed to download CRL for CA=" + caName
				+ " @ " + crlUrl);
		this.failures++;
	}
}
