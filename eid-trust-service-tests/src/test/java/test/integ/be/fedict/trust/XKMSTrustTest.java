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

package test.integ.be.fedict.trust;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import be.fedict.trust.client.SSLTrustManager;
import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.service.TrustServiceConstants;

import com.sun.xml.internal.ws.client.ClientTransportException;

/**
 * eID Trust Service XKMS2 Integration Tests.
 * 
 * @author fcorneli
 * 
 */
public class XKMSTrustTest {

	private static final Log LOG = LogFactory.getLog(XKMSTrustTest.class);

	private static final String hostname = "localhost";
	private static final int port = 8443;

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testValidateUnilateralTLSTrustFail() throws Exception {
		LOG.debug("validate using unilateral TLS Trust, fail.");

		// Setup
		KeyPair keyPair = generateKeyPair();

		/*
		 * Override default verification that CN of server SSL certificate has
		 * to be equal to the hostname.
		 */
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				if (hostname.equals(XKMSTrustTest.hostname)) {
					return true;
				}
				return false;
			}
		});

		// setup
		List<X509Certificate> signCertificateChain = XKMSTest
				.getSignCertificateChain();
		XKMS2Client client = new XKMS2Client("https://" + hostname + ":" + port);
		client.setServicePublicKey(keyPair.getPublic());

		/*
		 * Operate: validate non repudiation
		 */
		try {
			client
					.validate(
							TrustServiceConstants.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
							signCertificateChain);
			fail();
		} catch (ClientTransportException e) {
			// expected
		}
	}

	private KeyPair generateKeyPair() throws NoSuchAlgorithmException,
			InvalidAlgorithmParameterException {

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		SecureRandom random = new SecureRandom();
		keyPairGenerator.initialize(new RSAKeyGenParameterSpec(1024,
				RSAKeyGenParameterSpec.F4), random);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		return keyPair;
	}

	@Test
	public void testValidateUnilateralTLSTrust() throws Exception {
		LOG.debug("validate using unilateral TLS Trust.");

		// Retrieve server public key
		SSLTrustManager.reset();
		SSLTrustManager.initialize();
		SSLSocketFactory factory = HttpsURLConnection
				.getDefaultSSLSocketFactory();
		SSLSocket socket = (SSLSocket) factory.createSocket(hostname, port);
		socket.startHandshake();
		Certificate[] serverCerts = socket.getSession().getPeerCertificates();
		PublicKey publicKey = serverCerts[0].getPublicKey();
		LOG.debug("server public key: " + publicKey);
		socket.close();

		/*
		 * Override default verification that CN of server SSL certificate has
		 * to be equal to the hostname.
		 */
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				if (hostname.equals(XKMSTrustTest.hostname)) {
					return true;
				}
				return false;
			}
		});

		// setup
		SSLTrustManager.reset();
		List<X509Certificate> signCertificateChain = XKMSTest
				.getSignCertificateChain();
		XKMS2Client client = new XKMS2Client("https://" + hostname + ":" + port);
		client.setServicePublicKey(publicKey);

		/*
		 * Operate: validate non repudiation
		 */
		boolean result = client.validate(
				TrustServiceConstants.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
				signCertificateChain);

		// verify
		assertTrue(result);
	}
}
