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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;
import test.integ.be.fedict.performance.util.*;
import test.integ.be.fedict.trust.util.TestUtils;

import javax.swing.*;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

public class BeIdPerformanceTest implements PerformanceTest {

	private static final Log LOG = LogFactory.getLog(BeIdPerformanceTest.class);

	// private static final String XKMS_LOCATION =
	// "http://www.e-contract.be/eid-trust-service-ws/xkms2";

	// private static final String XKMS_LOCATION =
	// "http://192.168.1.101/eid-trust-service-ws/xkms2";
	private static final String XKMS_LOCATION = "https://trust-ws.ta.belgium.be/eid-trust-service-ws/xkms2";
	// private static final String XKMS_LOCATION =
	// "http://localhost/eid-trust-service-ws/xkms2";
	// private static final String XKMS_LOCATION =
	// "http://sebeco-dev-11:8080/eid-trust-service-ws/xkms2";
	private static final String PROXY_HOST = "proxy.yourict.net";
	// private static final String PROXY_HOST = null;
	private static final int PROXY_PORT = 8080;

	private static final int INTERVAL_SIZE = 1000 * 10;

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	private boolean run = true;
	private int count = 0;
	private int intervalCount = 0;

	/**
	 * {@inheritDoc}
	 */
	public int getIntervalCount() {
		return this.intervalCount;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getCount() {
		return this.count;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getRevokedCount() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRunning() {
		return this.run;
	}

	/**
	 * {@inheritDoc}
	 */
	public void stop() {
		this.run = false;
	}

	@Test
	public void testValidateEIDCertificate() throws Exception {
		LOG.debug("validate eID authentication certificate.");

		JOptionPane.showMessageDialog(null, "insert your eID card...");

		List<X509Certificate> authnCertificateChain = TestUtils
				.getAuthnCertificateChain();

		JOptionPane.showMessageDialog(null, "OK to remove eID card...");

		if (null != PROXY_HOST) {
			System.setProperty("http.proxyHost", PROXY_HOST);
			System.setProperty("http.proxyPort", Integer.toString(PROXY_PORT));
			System.setProperty("https.proxyHost", PROXY_HOST);
			System.setProperty("https.proxyPort", Integer.toString(PROXY_PORT));
		}

		XKMS2Client client = new XKMS2Client(XKMS_LOCATION);
		if (null != PROXY_HOST) {
			client.setProxy(PROXY_HOST, PROXY_PORT);
		}

		List<PerformanceData> performance = new LinkedList<PerformanceData>();
		PerformanceData currentPerformance = new PerformanceData();
		performance.add(currentPerformance);
		long nextIntervalT = System.currentTimeMillis() + INTERVAL_SIZE;

		new PerformanceWorkingFrame(this);

		while (this.run) {
			try {
				client.validate(authnCertificateChain);
				currentPerformance.inc();
				this.count++;
				if (System.currentTimeMillis() > nextIntervalT) {
					currentPerformance = new PerformanceData();
					nextIntervalT = System.currentTimeMillis() + INTERVAL_SIZE;
					performance.add(currentPerformance);
					this.intervalCount++;
				}
			} catch (Exception e) {
				LOG.error("error: " + e.getMessage(), e);
				currentPerformance.incFailures();
			}
		}

		PerformanceResultDialog dialog = new PerformanceResultDialog(
				new PerformanceResultsData(INTERVAL_SIZE, performance, 0, null));
		while (dialog.isVisible()) {
			Thread.sleep(1000);
		}
	}
}
