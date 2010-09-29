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
import test.integ.be.fedict.performance.util.PerformanceData;
import test.integ.be.fedict.performance.util.PerformanceResultDialog;
import test.integ.be.fedict.performance.util.PerformanceTest;
import test.integ.be.fedict.performance.util.PerformanceWorkingFrame;

import javax.swing.*;
import java.security.Security;
import java.util.LinkedList;
import java.util.List;

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

    @Test
    public void testPki() throws Exception {

        LOG.debug("performance test using test PKI");

        // get test PKI information
        String testPkiPath = JOptionPane.showInputDialog("Please give the test PKI base URL");
        // TODO

        // initialize XKMS2 client
        XKMS2Client client = new XKMS2Client(XKMS_LOCATION);

        // initialize test framework
        List<PerformanceData> performance = new LinkedList<PerformanceData>();
        PerformanceData currentPerformance = new PerformanceData();
        performance.add(currentPerformance);
        long nextIntervalT = System.currentTimeMillis() + INTERVAL_SIZE;

        new PerformanceWorkingFrame(this);

        // operate
        while (this.run) {
            try {
                // TODO
                //client.validate(authnCertificateChain);
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

        // show result
        PerformanceResultDialog dialog = new PerformanceResultDialog(
                INTERVAL_SIZE, performance);
        while (dialog.isVisible()) {
            Thread.sleep(1000);
        }
    }

    public int getIntervalCount() {
        return this.intervalCount;
    }

    public int getCount() {
        return this.count;
    }

    public boolean isRunning() {
        return this.run;
    }

    public void stop() {
        this.run = false;
    }
}
