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
import org.bouncycastle.x509.X509V2CRLGenerator;
import org.joda.time.DateTime;
import test.integ.be.fedict.performance.servlet.CrlServlet;
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

    private CAConfiguration root;
    private List<CAConfiguration> childs;
    private KeyPair keyPair;
    private X509Certificate certificate;
    private X509CRL crl;

    private X509V2CRLGenerator crlGenerator;

    public CAConfiguration(String name, long crlRecords) {

        this.name = name;
        this.crlRecords = crlRecords;
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

    public CAConfiguration getRoot() {
        return root;
    }

    public void setRoot(CAConfiguration root) {
        this.root = root;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public X509CRL getCrl() {
        return crl;
    }

    public X509V2CRLGenerator getCrlGenerator() {
        return crlGenerator;
    }

    public List<CAConfiguration> getChilds() {
        return childs;
    }

    public void generate() throws Exception {

        LOG.debug("generate CA " + this.name);

        keyPair = TestUtils.generateKeyPair();

        if (null == this.root) {
            this.certificate = generateCertificate(this.keyPair.getPublic(), this.keyPair.getPrivate());
        } else {
            this.certificate = generateCertificate(this.keyPair.getPublic(), this.root.getKeyPair().getPrivate());
        }

        // crl
        this.crlGenerator = createCrlGenerator();
        this.crl = this.crlGenerator.generate(this.keyPair.getPrivate());

        // generate childs
        for (CAConfiguration child : childs) {
            child.generate();
        }
    }

    private X509Certificate generateCertificate(PublicKey publicKey, PrivateKey privateKey)
            throws Exception {

        DateTime now = new DateTime();
        DateTime notBefore = now.minusYears(10);
        DateTime notAfter = now.plusYears(10);

        return TestUtils.generateCertificate(publicKey,
                "CN=" + name, privateKey, null, notBefore,
                notAfter, "SHA512WithRSAEncryption", true, true, false, null,
                CrlServlet.getPath(name), new KeyUsage(
                        KeyUsage.cRLSign));
    }

    private X509V2CRLGenerator createCrlGenerator() throws Exception {

        DateTime now = new DateTime();
        DateTime thisUpdate = now.minusHours(3);
        DateTime nextUpdate = now.plusHours(3);

        List<BigInteger> revokedSerialNumbers = new LinkedList<BigInteger>();
        for (long i = 0; i < this.crlRecords; i++) {
            revokedSerialNumbers.add(new BigInteger(Long.toString(i)));
        }

        //        crlGenerator.addExtension(X509Extensions.CRLNumber, false,
        //                new CRLNumber(BigInteger.ONE));


        return TestUtils.getCrlGenerator(certificate, thisUpdate, nextUpdate, revokedSerialNumbers);
    }
}
