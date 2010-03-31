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

import java.math.BigInteger;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.junit.Before;
import org.junit.Test;

import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.service.TrustServiceConstants;

/**
 * TSA Test.
 * 
 * @author wvdhaute
 * 
 */
public class TSATest {

	private static final Log LOG = LogFactory.getLog(TSATest.class);

	private static final String location = "http://sebeco-dev-11:8080";
	private static final String tsa_location = "http://tsa.belgium.be/connect";

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testTSA() throws Exception {

		// setup
		TimeStampRequestGenerator requestGen = new TimeStampRequestGenerator();
		requestGen.setCertReq(true);
		TimeStampRequest request = requestGen.generate(TSPAlgorithms.SHA1,
				new byte[20], BigInteger.valueOf(100));
		byte[] requestData = request.getEncoded();

		HttpClient httpClient = new HttpClient();
		PostMethod postMethod = new PostMethod(tsa_location);
		postMethod.setRequestEntity(new ByteArrayRequestEntity(requestData,
				"application/timestamp-query"));

		// operate
		int statusCode = httpClient.executeMethod(postMethod);
		if (statusCode != HttpStatus.SC_OK) {
			LOG.error("Error contacting TSP server " + tsa_location);
			throw new Exception("Error contacting TSP server " + tsa_location);
		}

		TimeStampResponse tspResponse = new TimeStampResponse(postMethod
				.getResponseBodyAsStream());
		postMethod.releaseConnection();

		CertStore certStore = tspResponse.getTimeStampToken()
				.getCertificatesAndCRLs("Collection", "BC");

		Collection<? extends Certificate> certificates = certStore
				.getCertificates(null);
		for (Certificate certificate : certificates) {
			LOG.debug("certificate: "
					+ ((X509Certificate) certificate).toString());
		}

		// send token to trust service
		XKMS2Client client = new XKMS2Client(location);
		client.validate(TrustServiceConstants.BELGIAN_TSA_TRUST_DOMAIN,
				tspResponse.getTimeStampToken());
	}
}
