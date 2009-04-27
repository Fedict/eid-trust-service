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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;

import javax.smartcardio.CardException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.eid.applet.Messages;
import be.fedict.eid.applet.PcscEid;
import be.fedict.eid.applet.PcscEidSpi;
import be.fedict.eid.applet.Status;
import be.fedict.eid.applet.View;
import be.fedict.trust.client.XKMS2Client;

/**
 * eID Trust Service XKMS2 Integration Tests.
 * 
 * @author fcorneli
 * 
 */
public class XKMSTest {

	private static final Log LOG = LogFactory.getLog(XKMSTest.class);

	@Test
	public void testValidateEIDCertificate() throws Exception {
		LOG.debug("validate eID certificate.");

		List<X509Certificate> authnCertificateChain = getAuthnCertificateChain();

		XKMS2Client client = new XKMS2Client();
		boolean result = client.validate(authnCertificateChain);
		LOG.debug("validation result: " + result);
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
