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

import be.fedict.trust.client.TrustServiceDomains;
import be.fedict.trust.client.WSSecurityClientHandler;
import be.fedict.trust.client.XKMS2Client;
import com.sun.xml.ws.client.ClientTransportException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;
import test.integ.be.fedict.trust.util.SSLTrustManager;
import test.integ.be.fedict.trust.util.TestUtils;

import javax.net.ssl.*;
import javax.xml.ws.soap.SOAPFaultException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * eID Trust Service XKMS2 Integration Tests.
 * <p/>
 * <p/>
 * !! Important !!
 * WS-Security needs to be setup on the eID Trust Service Host testing against for all tests to fail.
 *
 * @author wvdhaute
 */
public class XKMSTrustTest {

    private static final Log LOG = LogFactory.getLog(XKMSTrustTest.class);

    private static final int port = 8443;

    @Before
    public void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testValidateUnilateralTLSTrustFail() throws Exception {
        LOG.debug("validate using unilateral TLS Trust, should fail.");

        // Setup
        KeyPair keyPair = TestUtils.generateKeyPair();

        /*
           * Override default verification that CN of server SSL certificate has
           * to be equal to the hostname.
           */
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                if (TestUtils.XKMS_WS_HOST.equals(hostname)) {
                    return true;
                }
                return false;
            }
        });

        // setup
        List<X509Certificate> signCertificateChain = TestUtils
                .getSignCertificateChain();
        XKMS2Client client = new XKMS2Client("https://"
                + TestUtils.XKMS_WS_HOST + ":" + port
                + TestUtils.XKMS_WS_CONTEXT_PATH);
        client.setServicePublicKey(keyPair.getPublic());

        /*
           * Operate: validate non repudiation
           */
        try {
            client
                    .validate(
                            TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                            signCertificateChain);
            fail();
        } catch (ClientTransportException e) {
            // expected
        }
    }

    @Test
    public void testValidateUnilateralTLSTrust() throws Exception {
        LOG.debug("validate using unilateral TLS Trust.");

        // Retrieve server public key
        SSLTrustManager.initialize();
        SSLSocketFactory factory = HttpsURLConnection
                .getDefaultSSLSocketFactory();
        SSLSocket socket = (SSLSocket) factory.createSocket(
                TestUtils.XKMS_WS_HOST, port);
        socket.startHandshake();
        Certificate[] serverCerts = socket.getSession().getPeerCertificates();
        PublicKey publicKey = serverCerts[0].getPublicKey();
        LOG.debug("server public key: " + publicKey);
        socket.close();

        /*
           * Override default verification that CN of server SSL certificate has
           * to be equal to the hostname.
           */
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return hostname.equals(TestUtils.XKMS_WS_HOST);
            }
        });

        // setup
        List<X509Certificate> signCertificateChain = TestUtils
                .getSignCertificateChain();
        XKMS2Client client = new XKMS2Client("https://"
                + TestUtils.XKMS_WS_HOST + ":" + port
                + TestUtils.XKMS_WS_CONTEXT_PATH);
        client.setServicePublicKey(publicKey);

        /*
           * Operate: validate non repudiation
           */
        client.validate(
                TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                signCertificateChain);
    }

    @Test
    public void testValidateWSSecurityFails() throws Exception {

        LOG
                .debug("validate using WS-Security, fails due to certificate mismatch");

        // setup
        KeyPair fooKeyPair = TestUtils.generateKeyPair();
        X509Certificate fooCertificate = TestUtils
                .generateSelfSignedCertificate(fooKeyPair, "CN=f00");

        List<X509Certificate> signCertificateChain = TestUtils
                .getSignCertificateChain();
        XKMS2Client client = new XKMS2Client(TestUtils.XKMS_WS_LOCATION);
        client.setServerCertificate(fooCertificate);

        /*
           * Operate: validate non repudiation
           */
        try {
            client
                    .validate(
                            TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                            signCertificateChain);
            fail();
        } catch (SOAPFaultException e) {
            // expected
            assertEquals(WSSecurityClientHandler.ERROR_CERTIFICATE_MISMATCH, e
                    .getMessage());
        }

    }

}
