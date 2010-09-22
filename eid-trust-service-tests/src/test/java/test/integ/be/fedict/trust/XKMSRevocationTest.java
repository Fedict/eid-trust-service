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
import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.client.exception.RevocationDataCorruptException;
import be.fedict.trust.client.exception.RevocationDataNotFoundException;
import be.fedict.trust.client.exception.ValidationFailedException;
import be.fedict.trust.client.jaxb.xades132.EncapsulatedPKIDataType;
import be.fedict.trust.client.jaxb.xades132.RevocationValuesType;
import be.fedict.trust.xkms2.XKMSConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.ocsp.SingleResp;
import org.junit.Before;
import org.junit.Test;
import test.integ.be.fedict.trust.util.TestUtils;

import java.io.ByteArrayInputStream;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.junit.Assert.*;

/**
 * eID Trust Service XKMS2 Integration Tests.
 *
 * @author wvdhaute
 */
public class XKMSRevocationTest {

    private static final Log LOG = LogFactory.getLog(XKMSRevocationTest.class);

    private List<X509Certificate> signCertificateChain;

    private XKMS2Client client;

    @Before
    public void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        signCertificateChain = TestUtils.getSignCertificateChain();
        client = new XKMS2Client(TestUtils.XKMS_WS_LOCATION);
    }

    @Test
    public void testHistoricalValidationCorruptRevocationData()
            throws Exception {

        LOG.debug("historical validation with corrupt revocation data.");

        // setup
        Date validationDate = new Date();
        byte[] corruptOcspResponse = new byte[]{'f', 'o', 'o'};
        byte[] corruptCrl = new byte[]{'f', 'o', 'o'};

        // operate: validate with corrupt OCSP response
        try {
            client.validateEncoded(
                    TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                    signCertificateChain, validationDate,
                    Collections.singletonList(corruptOcspResponse),
                    new LinkedList<byte[]>());
            fail();
        } catch (RevocationDataCorruptException e) {
            // expected
        }

        // operate: validate with corrupt CRL
        try {
            client.validateEncoded(
                    TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                    signCertificateChain, validationDate,
                    new LinkedList<byte[]>(),
                    Collections.singletonList(corruptCrl));
            fail();
        } catch (RevocationDataCorruptException e) {
            // expected
        }

    }

    @Test
    public void testHistoricalValidationWithoutRevocationData()
            throws Exception {

        LOG.debug("historical validation without return revocation data.");

        // setup
        Date validationDate = new Date();

        // operate: validate without passing revocation data.
        try {
            client.validate(
                    TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                    signCertificateChain, validationDate, new LinkedList<OCSPResp>(), new LinkedList<X509CRL>());
            fail();
        } catch (RevocationDataNotFoundException e) {
            // expected
        }
        try {
            client.validate(
                    TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                    signCertificateChain, validationDate, null, null);
            fail();
        } catch (RevocationDataNotFoundException e) {
            // expected
        }

        // operate: validate without passing encoded revocation data.
        try {
            client.validateEncoded(
                    TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                    signCertificateChain, validationDate, new LinkedList<byte[]>(), new LinkedList<byte[]>());
            fail();
        } catch (RevocationDataNotFoundException e) {
            // expected
        }
        try {
            client.validateEncoded(
                    TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                    signCertificateChain, validationDate, null, null);
            fail();
        } catch (RevocationDataNotFoundException e) {
            // expected
        }

        // operate: validate without passing revocation values.
        try {
            client.validate(
                    TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                    signCertificateChain, validationDate, null);
            fail();
        } catch (RevocationDataNotFoundException e) {
            // expected
        }

    }

    @Test
    public void testValidateNonRepudiationEIDCertificateReturnRevocationDataThenValidateHistorically()
            throws Exception {
        LOG
                .debug("validate eID non repudiation certificate and return revocation data.");

        // setup
        Date validationDate = new Date();

        /*
           * Operate: validate non repudiation and return used revocation data
           */
        client.validate(
                TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                signCertificateChain, true);

        // verify
        RevocationValuesType revocationValues = client.getRevocationValues();
        assertNotNull(revocationValues);
        assertNotNull(revocationValues.getOCSPValues());
        assertNotNull(revocationValues.getCRLValues());
        assertEquals(1, revocationValues.getOCSPValues()
                .getEncapsulatedOCSPValue().size());
        assertEquals(1, revocationValues.getCRLValues()
                .getEncapsulatedCRLValue().size());

        // verify OCSP response revocation data
        EncapsulatedPKIDataType ocspData = revocationValues.getOCSPValues()
                .getEncapsulatedOCSPValue().get(0);
        OCSPResp ocspResp = new OCSPResp(ocspData.getValue());
        assertNotNull(ocspResp);
        assertEquals(OCSPResponseStatus.SUCCESSFUL, ocspResp.getStatus());
        BasicOCSPResp basicOCSPResp = (BasicOCSPResp) ocspResp
                .getResponseObject();
        assertNotNull(basicOCSPResp);
        assertEquals(1, basicOCSPResp.getResponses().length);
        for (SingleResp singleResp : basicOCSPResp.getResponses()) {
            assertNull(singleResp.getCertStatus());
        }

        // verify CRL revocation data
        EncapsulatedPKIDataType crlData = revocationValues.getCRLValues()
                .getEncapsulatedCRLValue().get(0);
        CertificateFactory certificateFactory = CertificateFactory.getInstance(
                "X.509", "BC");
        ByteArrayInputStream bais = new ByteArrayInputStream(crlData.getValue());
        X509CRL crl = (X509CRL) certificateFactory.generateCRL(bais);
        assertNotNull(crl);

        /*
           * Operate: historical validation of non repudiation with just returned
           * used revocation data (indirect, use list of ocsp resonses and crls )
           */
        client.validate(
                TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                signCertificateChain, validationDate, revocationValues);

        /*
           * Operate: historical validation of non repudiation with just returned
           * used revocation data (direct, append the RevocationValuesType object
           * returned by earlier call)
           */
        client.validate(
                TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                signCertificateChain, validationDate, Collections
                        .singletonList(ocspResp), Collections
                        .singletonList(crl));

        // setup
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(validationDate);
        calendar.add(Calendar.YEAR, -1);

        /*
           * Operate: historical validation of non repudiation with just returned
           * used revocation data and year old validation date
           */
        try {
            client
                    .validate(
                            TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
                            signCertificateChain, calendar.getTime(),
                            Collections.singletonList(ocspResp), Collections
                                    .singletonList(crl));
            fail();
        } catch (ValidationFailedException e) {
            // expected
            assertEquals(XKMSConstants.KEY_BINDING_REASON_VALIDITY_INTERVAL_URI, e
                    .getReasons().get(0));
        }
    }
}
