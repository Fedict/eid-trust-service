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
import be.fedict.trust.client.exception.ValidationFailedException;
import be.fedict.trust.xkms2.XKMSConstants;
import org.apache.commons.httpclient.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import test.integ.be.fedict.performance.servlet.CrlServlet;
import test.integ.be.fedict.performance.servlet.OcspServlet;
import test.integ.be.fedict.performance.util.*;
import test.integ.be.fedict.trust.util.TestUtils;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.swing.*;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Performance test using a configurable {@link TestPKI}.
 * <p/>
 * The performance test can run in interactive or non-interactive mode.
 * <p/>
 * In interactive mode, the base URL of the {@link TestPKI} will be asked first
 * and you have the ability to manually end the test and start generating the
 * graphs.
 * <p/>
 * In non-interactive mode you will have to specify the {@link TestPKI} in the
 * code and also specify the # minutes to run. When the test has finished, it
 * will dump the performanc result data in a file name
 * <code>performance_results_xxx.data</code>. Use the {@link TestLoadResults}
 * unit test to load this file in to get the graphs.
 * <p/>
 * <u>How to start the {@link TestPKI} ?</u> Have a look at
 * {@link TestPKIStartup} and {@link TestBeIdPKIStartup} for examples.
 * <p>
 * /> <u>How to configure the {@link TestPKI} ?</u> You can view its
 * configuration @ e.g. localhost:13456/configuration. There you will be able to
 * generate the {@link TestPKI} infrastructure. After this is done, all
 * certificates and initial CRLs will be generated and you are set to start your
 * performance test.
 */
public class TestPKIPerformanceTest implements PerformanceTest {

	private static final Log LOG = LogFactory
			.getLog(TestPKIPerformanceTest.class);

	private static final String HOST = "sebeco-dev-12";
	private static final String XKMS_LOCATION = "http://" + HOST
			+ ":8080/eid-trust-service-ws/xkms2";

	private static final int INTERVAL_SIZE = 1000 * 60 * 30; // ms

	private static boolean interactive = false;
	private static String PKI_PATH = "http://sebeco-dev-11:50292";
	private static int minutes = 60 * 48;

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	private boolean run = true;
	private int count = 0;
	private int intervalCount = 0;

	private DateTime startTime;

	// test configuration
	private int revokedPercentage = 10;
	private int expectedRevokedCount = 0;
	private int revokedCount = 0;

	private TestPKI testPKI;
	private String testPkiPath;

	private MBeanServerConnection rmi;

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
		return this.revokedCount;
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
	public void testPki() throws Exception {

		LOG.debug("performance test using test PKI");

		// get test PKI information
		if (interactive) {
			testPkiPath = JOptionPane
					.showInputDialog("Please give the test PKI base URL");
		} else {
			testPkiPath = PKI_PATH;
		}
		testPKI = TestPKI.load(testPkiPath);

		// initialize test framework
		List<PerformanceData> performance = new LinkedList<PerformanceData>();
		List<MemoryData> memory = new LinkedList<MemoryData>();
		PerformanceData currentPerformance = new PerformanceData();
		performance.add(currentPerformance);
		long nextIntervalT = System.currentTimeMillis() + INTERVAL_SIZE;

		// initialize JBoss monitoring for memory usage
		String jnpLocation = "jnp://" + HOST + ":1099";
		Hashtable<String, String> environment = new Hashtable<String, String>();
		environment.put(Context.INITIAL_CONTEXT_FACTORY,
				"org.jnp.interfaces.NamingContextFactory");
		environment.put(Context.PROVIDER_URL, jnpLocation);
		rmi = (MBeanServerConnection) new InitialContext(environment)
				.lookup("jmx/invoker/RMIAdaptor");

		if (interactive) {
			new PerformanceWorkingFrame(this);
		}

		// used to generate our certificates
		DateTime notBefore = new DateTime().minusYears(10);
		DateTime notAfter = new DateTime().plusYears(10);
		KeyPair testKeyPair = TestUtils.generateKeyPair();
		List<CAConfiguration> leaves = testPKI.getLeaves();
		Random random = new Random();

		// operate
		this.startTime = new DateTime();
		while (this.run) {

			try {
				List<X509Certificate> certificateChain = getCertificateChain(
						testKeyPair, leaves, random, notBefore, notAfter);

				// initialize XKMS2 client
				XKMS2Client client = new XKMS2Client(XKMS_LOCATION);
				client.validate("performance", certificateChain);
				currentPerformance.inc();
				this.count++;

			} catch (ValidationFailedException e) {

				if (e.getReasons()
						.get(0)
						.equals(XKMSConstants.KEY_BINDING_REASON_REVOCATION_STATUS_URI)) {
					LOG.debug("revoked");
					currentPerformance.incRevoked();
					this.revokedCount++;
				} else {
					LOG.error("Validation failed: " + e.getReasons().get(0));
					currentPerformance.incFailures();
				}

			} catch (Exception e) {
				LOG.error("error: " + e.getMessage(), e);
				currentPerformance.incFailures();
			} finally {

				if (System.currentTimeMillis() > nextIntervalT) {

					memory.add(new MemoryData(getFreeMemory(), getMaxMemory(),
							getTotalMemory()));

					currentPerformance = new PerformanceData();
					nextIntervalT = System.currentTimeMillis() + INTERVAL_SIZE;
					performance.add(currentPerformance);
					this.intervalCount++;

					if (!interactive) {
						DateTime now = new DateTime();
						if (now.isAfter(startTime.plusMinutes(minutes))) {
							this.run = false;
						}
					}

				}
			}
		}

		// add last performance
		performance.add(currentPerformance);

		if (interactive) {
			// show result
			PerformanceResultDialog dialog = new PerformanceResultDialog(
					new PerformanceResultsData(INTERVAL_SIZE, performance,
							this.expectedRevokedCount, memory));
			while (dialog.isVisible()) {
				Thread.sleep(1000);
			}
		} else {
			// write results to file for later
			PerformanceResultDialog.writeResults(new PerformanceResultsData(
					INTERVAL_SIZE, performance, expectedRevokedCount, memory));
		}
	}

	private long getFreeMemory() {

		try {
			return (Long) rmi.getAttribute(new ObjectName(
					"jboss.system:type=ServerInfo"), "FreeMemory");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1;
	}

	private long getMaxMemory() {

		try {
			return (Long) rmi.getAttribute(new ObjectName(
					"jboss.system:type=ServerInfo"), "MaxMemory");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1;
	}

	private long getTotalMemory() {

		try {
			return (Long) rmi.getAttribute(new ObjectName(
					"jboss.system:type=ServerInfo"), "TotalMemory");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1;
	}

	private List<X509Certificate> getCertificateChain(KeyPair testKeyPair,
			List<CAConfiguration> leaves, Random random, DateTime notBefore,
			DateTime notAfter) throws Exception {

		int leafIndex = random.nextInt(leaves.size());
		CAConfiguration ca = leaves.get(leafIndex);
		boolean revoked = random.nextInt(100) < revokedPercentage;
		long t = Math.abs(random.nextLong())
				% (0 != ca.getCrlRecords() ? ca.getCrlRecords() : 2);
		if (0 == t) {
			t = 1;
		}
		BigInteger serialNumber;
		if (revoked) {
			serialNumber = new BigInteger(Long.toString(t));
			expectedRevokedCount++;
		} else {
			serialNumber = new BigInteger(Long.toString(t + ca.getCrlRecords()));
		}

		String crlPath = new URI(testPkiPath + "/" + CrlServlet.PATH + "?"
				+ CrlServlet.CA_QUERY_PARAM + "=" + ca.getName(), false)
				.toString();
		String ocspPath = new URI(testPkiPath + "/" + OcspServlet.PATH + "?"
				+ OcspServlet.CA_QUERY_PARAM + "=" + ca.getName(), false)
				.toString();

		LOG.debug("generate for CA=" + ca.getName() + " revoked=" + revoked
				+ " sn=" + serialNumber);

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
