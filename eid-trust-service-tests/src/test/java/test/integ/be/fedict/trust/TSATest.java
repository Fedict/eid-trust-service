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

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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

import be.fedict.trust.client.TrustServiceDomains;
import be.fedict.trust.client.XKMS2Client;

/**
 * TSA Test.
 * 
 * @author wvdhaute
 */
public class TSATest {

	private static final Log LOG = LogFactory.getLog(TSATest.class);

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
		httpClient.getHostConfiguration().setProxy("proxy.yourict.net", 8080);
		PostMethod postMethod = new PostMethod(tsa_location);
		postMethod.setRequestEntity(new ByteArrayRequestEntity(requestData,
				"application/timestamp-query"));

		// operate
		int statusCode = httpClient.executeMethod(postMethod);
		if (statusCode != HttpStatus.SC_OK) {
			LOG.error("Error contacting TSP server " + tsa_location);
			throw new Exception("Error contacting TSP server " + tsa_location);
		}

		TimeStampResponse tspResponse = new TimeStampResponse(
				postMethod.getResponseBodyAsStream());
		postMethod.releaseConnection();

		CertStore certStore = tspResponse.getTimeStampToken()
				.getCertificatesAndCRLs("Collection", "BC");

		Collection<? extends Certificate> certificates = certStore
				.getCertificates(null);
		List<X509Certificate> certificateChain = new LinkedList<X509Certificate>();
		for (Certificate certificate : certificates) {
			LOG.debug("certificate: " + certificate.toString());
			certificateChain.add(0, (X509Certificate) certificate);
		}

		LOG.debug("token received");
		// send token to trust service
		XKMS2Client client = new XKMS2Client(
				"https://www.e-contract.be/eid-trust-service-ws/xkms2");
		client.setProxy("proxy.yourict.net", 8080);
		client.validate(TrustServiceDomains.BELGIAN_TSA_TRUST_DOMAIN,
				certificateChain, true);
	}

	@Test
	public void testNewTSACertificateChain2012() throws Exception {
		InputStream p7InputStream = TSATest.class
				.getResourceAsStream("/Fedict2012Chainpub.p7c");
		assertNotNull(p7InputStream);

		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		Collection<? extends Certificate> certificates = certificateFactory
				.generateCertificates(p7InputStream);
		List<X509Certificate> certificateChain = new LinkedList<X509Certificate>();
		LOG.debug("# of certificates: " + certificates.size());
		for (Certificate certificate : certificates) {
			LOG.debug("certificate: " + certificate);
			certificateChain.add(0, (X509Certificate) certificate);
		}

		XKMS2Client client = new XKMS2Client(
				"https://www.e-contract.be/eid-trust-service-ws/xkms2");
		client.setProxy("proxy.yourict.net", 8080);
		client.validate(TrustServiceDomains.BELGIAN_TSA_TRUST_DOMAIN,
				certificateChain);
	}
}
