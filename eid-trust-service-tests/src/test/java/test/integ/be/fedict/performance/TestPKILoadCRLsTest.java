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

package test.integ.be.fedict.performance;

import be.fedict.trust.client.XKMS2Client;
import org.apache.commons.httpclient.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import test.integ.be.fedict.performance.servlet.CrlServlet;
import test.integ.be.fedict.performance.servlet.OcspServlet;
import test.integ.be.fedict.trust.util.TestUtils;

import javax.management.MBeanServerConnection;
import javax.swing.*;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Integration test that does an XKMS request for each leaf CA in the
 * {@link TestPKI}. So to trigger the trust service loading all CRLs.
 */
public class TestPKILoadCRLsTest {

	private static final Log LOG = LogFactory.getLog(TestPKILoadCRLsTest.class);

	private static final String XKMS_LOCATION = "http://sebeco-dev-11:8080/eid-trust-service-ws/xkms2";

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	private TestPKI testPKI;
	private String testPkiPath;

	private MBeanServerConnection rmi;

	@Test
	public void testLoadCRLs() throws Exception {

		LOG.debug("load all test PKI CRLs");

		testPkiPath = JOptionPane
				.showInputDialog("Please give the test PKI base URL");
		testPKI = TestPKI.load(testPkiPath);

		// initialize XKMS2 client
		XKMS2Client client = new XKMS2Client(XKMS_LOCATION);

		// used to generate our certificates
		DateTime notBefore = new DateTime().minusYears(10);
		DateTime notAfter = new DateTime().plusYears(10);
		KeyPair testKeyPair = TestUtils.generateKeyPair();
		List<CAConfiguration> leaves = testPKI.getLeaves();
		Random random = new Random();

		// operate
		for (CAConfiguration ca : leaves) {

			List<X509Certificate> certificateChain = getCertificateChain(
					testKeyPair, ca, random, notBefore, notAfter);
			client.validate("performance", certificateChain);
		}
	}

	private List<X509Certificate> getCertificateChain(KeyPair testKeyPair,
			CAConfiguration ca, Random random, DateTime notBefore,
			DateTime notAfter) throws Exception {

		long t = Math.abs(random.nextLong())
				% (0 != ca.getCrlRecords() ? ca.getCrlRecords() : 2);
		if (0 == t) {
			t = 1;
		}
		BigInteger serialNumber = new BigInteger(Long.toString(t
				+ ca.getCrlRecords()));

		String crlPath = new URI(testPkiPath + "/" + CrlServlet.PATH + "?"
				+ CrlServlet.CA_QUERY_PARAM + "=" + ca.getName(), false)
				.toString();
		String ocspPath = new URI(testPkiPath + "/" + OcspServlet.PATH + "?"
				+ OcspServlet.CA_QUERY_PARAM + "=" + ca.getName(), false)
				.toString();

		LOG.debug("generate for CA=" + ca.getName() + " sn=" + serialNumber);

		X509Certificate certificate = TestUtils.generateCertificate(testKeyPair
				.getPublic(), "CN=Test", ca.getKeyPair().getPrivate(), ca
				.getCertificate(), notBefore, notAfter,
				"SHA512WithRSAEncryption", true, false, false, ocspPath,
				crlPath, null, serialNumber);

		List<X509Certificate> certificateChain = new LinkedList<X509Certificate>();
		certificateChain.add(certificate);
		certificateChain.add(ca.getCertificate());

		if (null != ca.getRoot()) {
			CAConfiguration parent = ca.getRoot();
			while (null != parent.getRoot()) {
				certificateChain.add(parent.getCertificate());
				parent = parent.getRoot();
			}
			certificateChain.add(parent.getCertificate());
		}
		return certificateChain;
	}
}
