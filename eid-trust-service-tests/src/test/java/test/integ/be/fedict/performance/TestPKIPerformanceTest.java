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
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import test.integ.be.fedict.performance.servlet.*;
import test.integ.be.fedict.performance.util.PerformanceData;
import test.integ.be.fedict.performance.util.PerformanceResultDialog;
import test.integ.be.fedict.performance.util.PerformanceTest;
import test.integ.be.fedict.performance.util.PerformanceWorkingFrame;
import test.integ.be.fedict.trust.util.TestUtils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class TestPKIPerformanceTest implements PerformanceTest {

    private static final Log LOG = LogFactory.getLog(TestPKIPerformanceTest.class);

    // private static final String XKMS_LOCATION =
    // "http://www.e-contract.be/eid-trust-service-ws/xkms2";

    //private static final String XKMS_LOCATION = "http://192.168.1.101/eid-trust-service-ws/xkms2";
    //private static final String XKMS_LOCATION = "http://localhost/eid-trust-service-ws/xkms2";
    private static final String XKMS_LOCATION = "http://sebeco-dev-11:8080/eid-trust-service-ws/xkms2";

    private static final int INTERVAL_SIZE = 1000 * 10;

    @Before
    public void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private boolean run = true;
    private int count = 0;
    private int intervalCount = 0;

    // test configuration
    private int revokedPercentage = 10;
    private int expectedRevokedCount = 0;
    private int revokedCount = 0;

    private TestPKI testPKI;
    private String testPkiPath;

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
        testPKI = new TestPKI();
        testPkiPath = JOptionPane.showInputDialog("Please give the test PKI base URL");

        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = new GetMethod(testPkiPath + "/"
                + ConfigurationServlet.PATH + "?"
                + ConfigurationServlet.ACTION + "=" + ConfigurationServlet.Action.GET);
        httpClient.executeMethod(getMethod);

        InputStream inputStream = getMethod.getResponseBodyAsStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String caString;
        while (null != (caString = br.readLine())) {
            String[] fields = caString.split(ConfigurationServlet.FIELD_SEPERATOR);
            testPKI.addSaveCa(fields[0], fields[1], Long.parseLong(fields[2]));
        }

        // now get private keys and certificates
        for (CAConfiguration rootCa : testPKI.getRoots().values()) {
            loadParentCa(httpClient, rootCa);
        }

        // initialize XKMS2 client
        XKMS2Client client = new XKMS2Client(XKMS_LOCATION);

        // initialize test framework
        List<PerformanceData> performance = new LinkedList<PerformanceData>();
        PerformanceData currentPerformance = new PerformanceData();
        performance.add(currentPerformance);
        long nextIntervalT = System.currentTimeMillis() + INTERVAL_SIZE;

        new PerformanceWorkingFrame(this);

        // used to generate our certificates
        DateTime now = new DateTime();
        DateTime notBefore = now.minusHours(10);
        DateTime notAfter = now.plusHours(10);
        KeyPair testKeyPair = TestUtils.generateKeyPair();
        List<CAConfiguration> leaves = testPKI.getLeaves();
        Random random = new Random();

        // operate
        while (this.run) {
            try {
                List<X509Certificate> certificateChain = getCertificateChain(testKeyPair,
                        leaves, random, notBefore, notAfter);
                client.validate("performance", certificateChain);
                currentPerformance.inc();
                this.count++;
                if (System.currentTimeMillis() > nextIntervalT) {
                    currentPerformance = new PerformanceData();
                    nextIntervalT = System.currentTimeMillis() + INTERVAL_SIZE;
                    performance.add(currentPerformance);
                    this.intervalCount++;
                }
            } catch (ValidationFailedException e) {

                if (e.getReasons().get(0).equals(XKMSConstants.KEY_BINDING_REASON_REVOCATION_STATUS_URI)) {
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
            }
        }

        // add last performance
        performance.add(currentPerformance);

        // show result
        PerformanceResultDialog dialog = new PerformanceResultDialog(
                INTERVAL_SIZE, performance, this.expectedRevokedCount);
        while (dialog.isVisible()) {
            Thread.sleep(1000);
        }
    }

    private List<X509Certificate> getCertificateChain(KeyPair testKeyPair,
                                                      List<CAConfiguration> leaves,
                                                      Random random,
                                                      DateTime notBefore,
                                                      DateTime notAfter)
            throws Exception {

        int leafIndex = random.nextInt(leaves.size());
        CAConfiguration ca = leaves.get(leafIndex);
        boolean revoked = random.nextInt(100) < revokedPercentage;
        long t = Math.abs(random.nextLong()) % ca.getCrlRecords();
        BigInteger serialNumber;
        if (revoked) {
            serialNumber = new BigInteger(Long.toString(t));
            expectedRevokedCount++;
        } else {
            serialNumber = new BigInteger(Long.toString(t + ca.getCrlRecords()));
        }

        String crlPath = testPkiPath
                + "/" + CrlServlet.PATH + "?"
                + CrlServlet.CA_QUERY_PARAM + "=" + ca.getName();
        String ocspPath = testPkiPath
                + "/" + OcspServlet.PATH + "?" + OcspServlet.CA_QUERY_PARAM + "=" + ca.getName();

        LOG.debug("generate for CA=" + ca.getName() + " revoked=" + revoked
                + " sn=" + serialNumber);

        X509Certificate certificate = TestUtils.generateCertificate(
                testKeyPair.getPublic(), "CN=Test",
                ca.getKeyPair().getPrivate(), ca.getCertificate(),
                notBefore, notAfter,
                "SHA512WithRSAEncryption", true, false, false,
                ocspPath, crlPath,
                null, serialNumber);

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

    private void loadParentCa(HttpClient httpClient, CAConfiguration parentCa) throws Exception {

        loadCa(httpClient, parentCa);
        for (CAConfiguration child : parentCa.getChilds()) {
            loadParentCa(httpClient, child);
        }
    }

    private void loadCa(HttpClient httpClient, CAConfiguration ca) throws Exception {

        LOG.debug("load CA: " + ca.getName());

        CertificateFactory certificateFactory = CertificateFactory
                .getInstance("X.509");

        // load certificate
        GetMethod getMethod = new GetMethod(testPkiPath + "/"
                + CertificateServlet.PATH + "?"
                + CertificateServlet.CA_QUERY_PARAM + "=" + ca.getName());
        httpClient.executeMethod(getMethod);

        X509Certificate certificate = (X509Certificate) certificateFactory
                .generateCertificate(getMethod.getResponseBodyAsStream());
        ca.setCertificate(certificate);

        // load private key
        getMethod = new GetMethod(testPkiPath + "/"
                + PrivateKeyServlet.PATH + "?"
                + PrivateKeyServlet.CA_QUERY_PARAM + "=" + ca.getName());
        httpClient.executeMethod(getMethod);

        PEMReader pemReader = new PEMReader(new InputStreamReader(getMethod
                .getResponseBodyAsStream()));
        KeyPair keyPair = (KeyPair) pemReader.readObject();
        ca.setKeyPair(keyPair);
    }
}
