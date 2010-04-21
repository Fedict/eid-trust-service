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

package test.integ.be.fedict.ca;

import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.integ.be.fedict.trust.util.TestUtils;
import be.fedict.trust.client.XKMS2Client;

public class CATest {

	private static String testCaPath = null;

	@BeforeClass
	public static void oneTimeSetUp() throws Exception {

		Security.addProvider(new BouncyCastleProvider());
		testCaPath = JOptionPane
				.showInputDialog("Please give the test CA's path: ( http://<host-name>:<port> )");
	}

	@AfterClass
	public static void oneTimeTearDown() throws Exception {

	}

	@Test
	public void testCA() throws Exception {

		// setup
		DateTime now = new DateTime();
		DateTime notBefore = now.minusHours(10);
		DateTime notAfter = now.plusHours(10);

		// fetch certificates and private key
		HttpClient httpClient = new HttpClient();
		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");

		GetMethod getMethod = new GetMethod(testCaPath
				+ TestCA.INTER_CA_CONTEXT_PATH);
		httpClient.executeMethod(getMethod);
		X509Certificate interCertificate = (X509Certificate) certificateFactory
				.generateCertificate(getMethod.getResponseBodyAsStream());

		getMethod = new GetMethod(testCaPath + TestCA.ROOT_CA_CONTEXT_PATH);
		httpClient.executeMethod(getMethod);
		X509Certificate rootCertificate = (X509Certificate) certificateFactory
				.generateCertificate(getMethod.getResponseBodyAsStream());

		getMethod = new GetMethod(testCaPath
				+ TestCA.INTER_CA_PRIVATE_KEY_CONTEXT_PATH);
		httpClient.executeMethod(getMethod);
		PEMReader pemReader = new PEMReader(new InputStreamReader(getMethod
				.getResponseBodyAsStream()));
		KeyPair interKeyPair = (KeyPair) pemReader.readObject();

		// generate test certificate chain
		KeyPair testKeyPair = TestUtils.generateKeyPair();
		X509Certificate certificate = TestUtils.generateCertificate(testKeyPair
				.getPublic(), "CN=Test", interKeyPair.getPrivate(),
				interCertificate, notBefore, notAfter,
				"SHA512WithRSAEncryption", true, false, false, testCaPath
						+ TestCA.INTER_CA_OCSP_CONTEXT_PATH, testCaPath
						+ TestCA.INTER_CA_CRL_CONTEXT_PATH, null);

		List<X509Certificate> certificateChain = new LinkedList<X509Certificate>();
		certificateChain.add(certificate);
		certificateChain.add(interCertificate);
		certificateChain.add(rootCertificate);

		XKMS2Client client = new XKMS2Client(TestUtils.XKMS_WS_LOCATION);

		// operate
		client.validate("test", certificateChain);
	}
}
