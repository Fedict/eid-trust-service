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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.ocsp.SingleResp;
import org.bouncycastle.util.encoders.Base64;
import org.etsi.uri._01903.v1_3.EncapsulatedPKIDataType;
import org.etsi.uri._01903.v1_3.RevocationValuesType;
import org.junit.Before;
import org.junit.Test;

import sun.security.x509.X509CRLImpl;
import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.xkms2.XKMSConstants;

/**
 * eID Trust Service XKMS2 Integration Tests.
 * 
 * @author wvdhaute
 * 
 */
public class XKMSRevocationTest {

	private static final Log LOG = LogFactory.getLog(XKMSRevocationTest.class);

	private static final String location = "http://sebeco-dev-11:8080";

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testValidateNonRepudiationEIDCertificateReturnRevocationDataThenValidateHistorically()
			throws Exception {
		LOG
				.debug("validate eID non repudiation certificate and return revocation data.");

		// setup
		List<X509Certificate> signCertificateChain = TestUtils
				.getSignCertificateChain();
		XKMS2Client client = new XKMS2Client(location);
		Date validationDate = new Date();

		/*
		 * Operate: validate non repudiation and return used revocation data
		 */
		boolean result = client.validate(
				TrustServiceConstants.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
				signCertificateChain, true);

		// verify
		assertTrue(result);
		RevocationValuesType revocationValues = client.getRevocationValues();
		assertNotNull(revocationValues);
		assertNotNull(revocationValues.getOCSPValues());
		assertNotNull(revocationValues.getCRLValues());
		assertEquals(1, revocationValues.getOCSPValues()
				.getEncapsulatedOCSPValue().size());
		assertEquals(1, revocationValues.getCRLValues()
				.getEncapsulatedCRLValue().size());

		// verify OCSP response revocation data
		EncapsulatedPKIDataType ocspData = revocationValues.getOCSPValues()
				.getEncapsulatedOCSPValue().get(0);
		OCSPResp ocspResp = new OCSPResp(Base64.decode(ocspData.getValue()));
		assertNotNull(ocspResp);
		assertEquals(OCSPResponseStatus.SUCCESSFUL, ocspResp.getStatus());
		BasicOCSPResp basicOCSPResp = (BasicOCSPResp) ocspResp
				.getResponseObject();
		assertNotNull(basicOCSPResp);
		assertEquals(1, basicOCSPResp.getResponses().length);
		for (SingleResp singleResp : basicOCSPResp.getResponses()) {
			assertNull(singleResp.getCertStatus());
		}

		// verify CRL revocation data
		EncapsulatedPKIDataType crlData = revocationValues.getCRLValues()
				.getEncapsulatedCRLValue().get(0);
		X509CRL crl = new X509CRLImpl(Base64.decode(crlData.getValue()));
		assertNotNull(crl);

		/*
		 * Operate: historical validation of non repudiation with just returned
		 * used revocation data (indirect, use list of ocsp resonses and crls )
		 */
		result = client.validate(
				TrustServiceConstants.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
				signCertificateChain, validationDate, revocationValues);

		// verify
		assertTrue(result);

		/*
		 * Operate: historical validation of non repudiation with just returned
		 * used revocation data (direct, append the RevocationValuesType object
		 * returned by earlier call)
		 */
		result = client.validate(
				TrustServiceConstants.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
				signCertificateChain, validationDate, Collections
						.singletonList(ocspResp), Collections
						.singletonList(crl));

		// verify
		assertTrue(result);

		// setup
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(validationDate);
		calendar.add(Calendar.YEAR, -1);

		/*
		 * Operate: historical validation of non repudiation with just returned
		 * used revocation data and year old validation date
		 */
		result = client.validate(
				TrustServiceConstants.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
				signCertificateChain, calendar.getTime(), Collections
						.singletonList(ocspResp), Collections
						.singletonList(crl));
		List<String> reasonURIs = client.getReasonURIs();

		// verify
		assertFalse(result);
		assertEquals(1, reasonURIs.size());
		assertEquals(XKMSConstants.KEY_BINDING_REASON_ISSUER_TRUST_URI,
				reasonURIs.get(0));
	}
}
