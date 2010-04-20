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

import static org.junit.Assert.fail;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.integ.be.fedict.ca.servlet.InterCACrlServlet;
import test.integ.be.fedict.ca.servlet.InterCAOcspServlet;
import test.integ.be.fedict.trust.util.TestUtils;
import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.client.exception.ValidationFailedException;

public class CATest {

	private static final Log LOG = LogFactory.getLog(CATest.class);

	private static TestCA testCA;

	@BeforeClass
	public static void oneTimeSetUp() throws Exception {

		testCA = new TestCA();
		testCA.start();

		LOG.debug("Root CA       : " + testCA.getPath()
				+ TestCA.ROOT_CA_CONTEXT_PATH);
		LOG.debug("Root CA CRL   : " + testCA.getPath()
				+ TestCA.ROOT_CA_CRL_CONTEXT_PATH);

		LOG.debug("Inter CA      : " + testCA.getPath()
				+ TestCA.INTER_CA_CONTEXT_PATH);
		LOG.debug("Inter CA CRL  : " + testCA.getPath()
				+ TestCA.INTER_CA_CRL_CONTEXT_PATH);
		LOG.debug("Inter CA OCSP : " + testCA.getPath()
				+ TestCA.INTER_CA_OCSP_CONTEXT_PATH);
		JOptionPane.showMessageDialog(null, "Test CA started...");
	}

	@AfterClass
	public static void oneTimeTearDown() throws Exception {

		testCA.stop();
	}

	@Test
	public void testCA() throws Exception {

		// setup
		DateTime now = new DateTime();
		DateTime notBefore = now.minusHours(10);
		DateTime notAfter = now.plusHours(10);

		KeyPair testKeyPair = TestUtils.generateKeyPair();
		X509Certificate certificate = TestUtils.generateCertificate(testKeyPair
				.getPublic(), "CN=Test", TestCA.interCaKeyPair.getPrivate(),
				TestCA.interCa, notBefore, notAfter, "SHA512WithRSAEncryption",
				true, false, false, testCA.getPath()
						+ TestCA.INTER_CA_OCSP_CONTEXT_PATH, testCA.getPath()
						+ TestCA.INTER_CA_CRL_CONTEXT_PATH, null);
		X509Certificate ocspRevokedCertificate = TestUtils.generateCertificate(
				testKeyPair.getPublic(), "CN=TestOcspRevoked",
				TestCA.interCaKeyPair.getPrivate(), TestCA.interCa, notBefore,
				notAfter, "SHA512WithRSAEncryption", true, false, false, testCA
						.getPath()
						+ TestCA.INTER_CA_OCSP_CONTEXT_PATH, testCA.getPath()
						+ TestCA.INTER_CA_CRL_CONTEXT_PATH, null);
		X509Certificate crlRevokedCertificate = TestUtils.generateCertificate(
				testKeyPair.getPublic(), "CN=TestCrlRevoked",
				TestCA.interCaKeyPair.getPrivate(), TestCA.interCa, notBefore,
				notAfter, "SHA512WithRSAEncryption", true, false, false, null,
				testCA.getPath() + TestCA.INTER_CA_CRL_CONTEXT_PATH, null);

		List<X509Certificate> certificateChain = new LinkedList<X509Certificate>();
		certificateChain.add(certificate);
		certificateChain.add(TestCA.interCa);
		certificateChain.add(TestCA.rootCa);

		InterCAOcspServlet.markRevoked(ocspRevokedCertificate, true);
		InterCACrlServlet.markRevoked(crlRevokedCertificate, true);

		XKMS2Client client = new XKMS2Client(TestUtils.XKMS_WS_LOCATION);

		// operate
		client.validate("test", certificateChain);

		// validate certificate, revoked via OCSPcertificate
		certificateChain.set(0, ocspRevokedCertificate);

		// operate
		try {
			client.validate("test", certificateChain);
			fail();
		} catch (ValidationFailedException e) {
			// expected
		}

		// revoke testCertificate using CRL
		certificateChain.set(0, crlRevokedCertificate);

		// operate
		try {
			client.validate("test", certificateChain);
			fail();
		} catch (ValidationFailedException e) {
			// expected
		}
	}
}
