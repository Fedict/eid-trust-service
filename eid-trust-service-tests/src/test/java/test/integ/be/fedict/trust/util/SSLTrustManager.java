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

package test.integ.be.fedict.trust.util;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test SSL Trust Manager that accepts all.
 * 
 * @author wvdhaute
 * 
 */
public class SSLTrustManager implements X509TrustManager {

	private static final Log LOG = LogFactory.getLog(SSLTrustManager.class);

	private SSLTrustManager() {

		// empty
	}

	private static SSLSocketFactory socketFactory;

	public static synchronized void initialize() {

		LOG.debug("initialize");
		if (null == socketFactory) {

			initSocketFactory();
			HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory);
		} else {
			if (false == socketFactory.equals(HttpsURLConnection
					.getDefaultSSLSocketFactory()))
				throw new RuntimeException("wrong SSL socket factory installed");
		}
	}

	private static void initSocketFactory() {

		LOG.debug("init socket factory");
		SSLTrustManager trustManagerInstance = new SSLTrustManager();
		TrustManager[] trustManager = { trustManagerInstance };
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			SecureRandom secureRandom = new SecureRandom();
			sslContext.init(null, trustManager, secureRandom);
			LOG.debug("SSL context provider: "
					+ sslContext.getProvider().getName());
			socketFactory = sslContext.getSocketFactory();
		} catch (KeyManagementException e) {
			String msg = "key management error: " + e.getMessage();
			LOG.error(msg, e);
			throw new RuntimeException(msg, e);
		} catch (NoSuchAlgorithmException e) {
			String msg = "TLS algo not present: " + e.getMessage();
			LOG.error(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {

		LOG.debug("Test SSL Trust Manager: checkServerTrusted");
		return;
	}

	/**
	 * {@inheritDoc}
	 */
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {

		LOG.debug("Test SSL Trust Manager: checkClientTrusted");
	}

	/**
	 * {@inheritDoc}
	 */
	public X509Certificate[] getAcceptedIssuers() {

		return null;
	}

}
