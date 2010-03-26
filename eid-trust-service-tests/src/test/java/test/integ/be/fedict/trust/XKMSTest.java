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

import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import be.fedict.trust.BelgianTrustValidatorFactory;
import be.fedict.trust.NetworkConfig;
import be.fedict.trust.TrustValidator;
import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.client.exception.TrustDomainNotFoundException;
import be.fedict.trust.service.TrustServiceConstants;

/**
 * eID Trust Service XKMS2 Integration Tests.
 * 
 * @author fcorneli
 * 
 */
public class XKMSTest {

	private static final Log LOG = LogFactory.getLog(XKMSTest.class);

	private static final String location = "http://localhost:8080";

	// private static final NetworkConfig NETWORK_CONFIG = new NetworkConfig(
	// "proxy.yourict.net", 8080);
	private static final NetworkConfig NETWORK_CONFIG = null;

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testValidateEIDCertificate() throws Exception {
		LOG.debug("validate eID authentication certificate.");

		List<X509Certificate> authnCertificateChain = TestUtils
				.getAuthnCertificateChain();

		XKMS2Client client = new XKMS2Client(location);
		boolean result = client.validate(authnCertificateChain);
		assertTrue(result);
	}

	@Test
	public void testValidateNonRepudiationEIDCertificate() throws Exception {
		LOG.debug("validate eID non repudiation certificate.");

		List<X509Certificate> signCertificateChain = TestUtils
				.getSignCertificateChain();

		XKMS2Client client = new XKMS2Client(location);
		boolean result = client.validate(
				TrustServiceConstants.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
				signCertificateChain);
		assertTrue(result);
	}

	@Test
	public void testValidateNationalRegistryEIDCertificate() throws Exception {
		LOG.debug("validate eID national registry certificate.");

		List<X509Certificate> nationalRegistryCertificateChain = TestUtils
				.getNationalRegistryCertificateChain();

		XKMS2Client client = new XKMS2Client(location);
		boolean result = client
				.validate(
						TrustServiceConstants.BELGIAN_EID_NATIONAL_REGISTRY_TRUST_DOMAIN,
						nationalRegistryCertificateChain);
		assertTrue(result);
	}

	@Test
	public void testValidateWrongTrustDomainEIDCertificate() throws Exception {
		LOG.debug("validate eID certificate with wrong trust domain.");

		List<X509Certificate> authnCertificateChain = TestUtils
				.getAuthnCertificateChain();

		XKMS2Client client = new XKMS2Client(location);
		try {
			client.validate("f00", authnCertificateChain);
		} catch (TrustDomainNotFoundException e) {
			// expected
			return;
		}
		fail();
	}

	@Test
	public void testValidateViaJTrust() throws Exception {
		LOG.debug("validate eID certificate via jTrust.");

		List<X509Certificate> authnCertificateChain = TestUtils
				.getAuthnCertificateChain();

		TrustValidator trustValidator = BelgianTrustValidatorFactory
				.createTrustValidator(NETWORK_CONFIG);

		trustValidator.isTrusted(authnCertificateChain);
		assertTrue(trustValidator.getResult().isValid());
	}

	@Test
	public void testValidateNonRepudiationViaJTrust() throws Exception {
		LOG.debug("validate eID non repudiation certificate via jTrust.");

		List<X509Certificate> signCertificateChain = TestUtils
				.getSignCertificateChain();

		TrustValidator trustValidator = BelgianTrustValidatorFactory
				.createNonRepudiationTrustValidator(NETWORK_CONFIG);

		trustValidator.isTrusted(signCertificateChain);
		assertTrue(trustValidator.getResult().isValid());
	}

	@Test
	public void testValidateNationalRegistryViaJTrust() throws Exception {
		LOG.debug("validate eID national registry certificate via jTrust.");

		List<X509Certificate> nationalRegistryCertificateChain = TestUtils
				.getNationalRegistryCertificateChain();

		TrustValidator trustValidator = BelgianTrustValidatorFactory
				.createNationalRegistryTrustValidator(NETWORK_CONFIG);

		trustValidator.isTrusted(nationalRegistryCertificateChain);
		assertTrue(trustValidator.getResult().isValid());
	}

	private static final int COUNT = 20;

	@Test
	public void testValidatePerformanceViaPKI() throws Exception {
		LOG.debug("validate eID certificate (performance) via jTrust.");

		List<X509Certificate> authnCertificateChain = TestUtils
				.getAuthnCertificateChain();

		TrustValidator trustValidator = BelgianTrustValidatorFactory
				.createTrustValidator(NETWORK_CONFIG);

		long t0 = System.currentTimeMillis();
		for (int idx = 0; idx < COUNT; idx++) {
			trustValidator.isTrusted(authnCertificateChain);
		}
		long t1 = System.currentTimeMillis();
		LOG.debug("dt: " + ((double) (t1 - t0)) / 1000);
	}

	@Test
	public void testValidatePerformanceViaTrustService() throws Exception {
		LOG.debug("validate eID authentication certificate (performance).");

		List<X509Certificate> authnCertificateChain = TestUtils
				.getAuthnCertificateChain();

		XKMS2Client client = new XKMS2Client(location);

		long t0 = System.currentTimeMillis();
		for (int idx = 0; idx < COUNT; idx++) {
			client.validate(authnCertificateChain);
		}
		long t1 = System.currentTimeMillis();
		LOG.debug("dt: " + ((double) (t1 - t0)) / 1000);
	}

}
