/*
 * eID Trust Service Project.
 * Copyright (C) 2009 FedICT.
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

import java.awt.Component;
import java.io.IOException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;

import javax.smartcardio.CardException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import be.fedict.eid.applet.Messages;
import be.fedict.eid.applet.PcscEid;
import be.fedict.eid.applet.PcscEidSpi;
import be.fedict.eid.applet.Status;
import be.fedict.eid.applet.View;
import be.fedict.trust.BelgianTrustValidatorFactory;
import be.fedict.trust.NetworkConfig;
import be.fedict.trust.TrustValidator;
import be.fedict.trust.client.XKMS2Client;

/**
 * eID Trust Service XKMS2 Integration Tests.
 * 
 * @author fcorneli
 * 
 */
public class XKMSTest {

	private static final Log LOG = LogFactory.getLog(XKMSTest.class);

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testValidateEIDCertificate() throws Exception {
		LOG.debug("validate eID certificate.");

		List<X509Certificate> authnCertificateChain = getAuthnCertificateChain();

		XKMS2Client client = new XKMS2Client();
		boolean result = client.validate(authnCertificateChain);
		LOG.debug("validation result: " + result);
	}

	private static final int COUNT = 20;

	@Test
	public void testValidatePerformanceViaPKI() throws Exception {
		LOG.debug("validate eID certificate.");

		List<X509Certificate> authnCertificateChain = getAuthnCertificateChain();

		NetworkConfig networkConfig = new NetworkConfig("proxy.yourict.net",
				8080);
		TrustValidator trustValidator = BelgianTrustValidatorFactory
				.createTrustValidator(networkConfig);

		long t0 = System.currentTimeMillis();
		for (int idx = 0; idx < COUNT; idx++) {
			trustValidator.isTrusted(authnCertificateChain);
		}
		long t1 = System.currentTimeMillis();
		LOG.debug("dt: " + ((double) (t1 - t0)) / 1000);
	}

	@Test
	public void testValidatePerformanceViaTrustService() throws Exception {
		LOG.debug("validate eID certificate.");

		List<X509Certificate> authnCertificateChain = getAuthnCertificateChain();

		XKMS2Client client = new XKMS2Client();

		long t0 = System.currentTimeMillis();
		for (int idx = 0; idx < COUNT; idx++) {
			client.validate(authnCertificateChain);
		}
		long t1 = System.currentTimeMillis();
		LOG.debug("dt: " + ((double) (t1 - t0)) / 1000);
	}

	private List<X509Certificate> getAuthnCertificateChain() throws Exception,
			CardException, IOException, CertificateException {
		Messages messages = new Messages(Locale.getDefault());
		View view = new LogTestView(LOG);
		PcscEidSpi pcscEid = new PcscEid(view, messages);

		if (false == pcscEid.isEidPresent()) {
			LOG.debug("insert eID card...");
			pcscEid.waitForEidPresent();
		}

		List<X509Certificate> authnCertificateChain;
		try {
			authnCertificateChain = pcscEid.getAuthnCertificateChain();
		} finally {
			pcscEid.close();
		}
		return authnCertificateChain;
	}

	private static class LogTestView implements View {

		private final Log log;

		public LogTestView(Log log) {
			this.log = log;
		}

		public void addDetailMessage(String message) {
			this.log.debug(message);
		}

		public Component getParentComponent() {
			return null;
		}

		public boolean privacyQuestion(boolean includeAddress,
				boolean includePhoto) {
			return true;
		}

		public void progressIndication(int max, int current) {
			this.log.debug("progress " + current + " of " + max);
		}

		public void setStatusMessage(Status status, String statusMessage) {
			this.log.debug(status.toString() + ": " + statusMessage);
		}
	}
}
