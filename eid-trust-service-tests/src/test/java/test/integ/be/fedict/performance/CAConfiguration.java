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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.joda.time.DateTime;
import test.integ.be.fedict.performance.servlet.CrlServlet;
import test.integ.be.fedict.performance.servlet.OcspServlet;
import test.integ.be.fedict.trust.util.TestUtils;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

public class CAConfiguration implements Serializable {

    private static final Log LOG = LogFactory.getLog(CAConfiguration.class);

    private final String name;
    private long crlRecords;
    private int crlRefresh; // in minutes

    private CAConfiguration root;
    private List<CAConfiguration> childs;
    private KeyPair keyPair;
    private X509Certificate certificate;
    private X509CRL crl;

    // CRL config data
    private DateTime crlNextUpdate;
    private int crlNumber = 1;

    public CAConfiguration(String name, long crlRecords, int crlRefresh) {

        this.name = name;
        this.crlRecords = crlRecords;
        this.crlRefresh = crlRefresh;
        this.childs = new LinkedList<CAConfiguration>();
    }

    public String getName() {
        return name;
    }

    public long getCrlRecords() {
        return crlRecords;
    }

    public void setCrlRecords(long crlRecords) {
        this.crlRecords = crlRecords;
    }

    public int getCrlRefresh() {
        return crlRefresh;
    }

    public void setCrlRefresh(int crlRefresh) {
        this.crlRefresh = crlRefresh;
    }

    public CAConfiguration getRoot() {
        return root;
    }

    public void setRoot(CAConfiguration root) {
        this.root = root;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public X509CRL getCrl() throws Exception {

        if (this.crlRefresh > 0) {
            DateTime now = new DateTime();
            if (now.isAfter(this.crlNextUpdate)) {
                // time's up, generate me a new one!
                this.crlNextUpdate = now.plusMinutes(this.crlRefresh);
                this.crlNumber++;
                LOG.debug("generate new CRL for CA=" + this.name + " (nextUpdate="
                        + this.crlNextUpdate.toString() + " crlNumber=" + this.crlNumber + ")");

                this.crl = generateCrl();
            }
        }

        return crl;
    }

    public List<CAConfiguration> getChilds() {
        return childs;
    }

    public void generate() throws Exception {

        keyPair = TestUtils.generateKeyPair();

        if (null == this.root) {
            LOG.debug("generate CA " + this.name + " (root)");
            this.certificate = generateCertificate(this.keyPair.getPublic(),
                    name, this.keyPair.getPrivate(), null, crlRecords);
        } else {
            LOG.debug("generate CA " + this.name + " (intermediate)");
            this.certificate = generateCertificate(this.keyPair.getPublic(),
                    this.root.getName(),
                    this.root.getKeyPair().getPrivate(),
                    this.root.getCertificate(),
                    this.root.getCrlRecords());
        }

        // crl
        this.crl = generateCrl();

        // generate childs
        for (CAConfiguration child : childs) {
            child.generate();
        }
    }

    private X509Certificate generateCertificate(PublicKey publicKey,
                                                String issuerName,
                                                PrivateKey issuerPrivateKey,
                                                X509Certificate issuerCertificate,
                                                long maxRevokedSn)
            throws Exception {

        DateTime now = new DateTime();
        DateTime notBefore = now.minusYears(10);
        DateTime notAfter = now.plusYears(10);

        return TestUtils.generateCertificate(
                publicKey, "CN=" + name,
                issuerPrivateKey, issuerCertificate,
                notBefore, notAfter, "SHA512WithRSAEncryption",
                true, true, false,
                OcspServlet.getPath(issuerName),
                CrlServlet.getPath(issuerName),
                new KeyUsage(KeyUsage.cRLSign),
                new BigInteger(Long.toString(maxRevokedSn + 1)));
    }

    private X509CRL generateCrl() throws Exception {

        DateTime now = new DateTime();
        if (this.crlRefresh > 0) {
            crlNextUpdate = now.plusMinutes(this.crlRefresh);
        } else {
            crlNextUpdate = now.plusHours(3);
        }


        List<BigInteger> revokedSerialNumbers = new LinkedList<BigInteger>();
        for (long i = 0; i < this.crlRecords; i++) {
            revokedSerialNumbers.add(new BigInteger(Long.toString(i)));
        }

        return TestUtils.generateCrl(crlNumber, this.keyPair.getPrivate(),
                certificate, now, crlNextUpdate, revokedSerialNumbers);
    }
}
