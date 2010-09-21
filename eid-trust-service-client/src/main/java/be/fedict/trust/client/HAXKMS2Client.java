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

package be.fedict.trust.client;

import be.fedict.trust.TrustValidator;
import be.fedict.trust.client.exception.RevocationDataNotFoundException;
import be.fedict.trust.client.exception.TrustDomainNotFoundException;
import be.fedict.trust.client.exception.ValidationFailedException;
import be.fedict.trust.client.jaxb.xades132.CertifiedRolesListType;
import be.fedict.trust.client.jaxb.xades132.EncapsulatedPKIDataType;
import be.fedict.trust.client.jaxb.xades132.RevocationValuesType;
import be.fedict.trust.xkms2.XKMSConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.tsp.TimeStampToken;

import javax.xml.ws.WebServiceException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * High Availability client component for the eID Trust Service XKMS2 web service.
 * <p/>
 * In case the eID Trust Service is not available, it will fall back to the specified jTrust {@link TrustValidator}.
 *
 * @author wvdhaute
 */
public class HAXKMS2Client extends XKMS2Client {

    private static final Log LOG = LogFactory.getLog(HAXKMS2Client.class);

    private final TrustValidator trustValidator;

    /**
     * Main constructor
     *
     * @param location       location ( complete path ) of the XKMS2 web service
     * @param trustValidator Backup {@link TrustValidator} in case the XKMS2 service @ location is not available.
     */
    public HAXKMS2Client(String location, TrustValidator trustValidator) {
        super(location);

        this.trustValidator = trustValidator;
    }

    @Override
    protected void validate(String trustDomain,
                            List<X509Certificate> certificateChain,
                            boolean returnRevocationData, Date validationDate,
                            List<byte[]> ocspResponses, List<byte[]> crls,
                            RevocationValuesType revocationValues,
                            TimeStampToken timeStampToken,
                            CertifiedRolesListType attributeCertificates)
            throws CertificateEncodingException, ValidationFailedException,
            TrustDomainNotFoundException, RevocationDataNotFoundException {

        try {
            super.validate(trustDomain, certificateChain, returnRevocationData,
                    validationDate, ocspResponses, crls,
                    revocationValues, timeStampToken, attributeCertificates);
        } catch (WebServiceException e) {

            // fallback to specified trust validator.
            fallbackValidate(certificateChain, validationDate, attributeCertificates);
        }
    }

    private void fallbackValidate(List<X509Certificate> certificateChain, Date validationDate,
                                  CertifiedRolesListType attributeCertificates) throws ValidationFailedException {

        LOG.debug("eID Trust Service not available, falling back to specified Trust Validator");

        try {
            if (null != attributeCertificates) {
                List<byte[]> encodedAttributeCertificates = new LinkedList<byte[]>();
                for (EncapsulatedPKIDataType attributeCertificate : attributeCertificates.getCertifiedRole()) {
                    encodedAttributeCertificates.add(attributeCertificate.getValue());
                }

                if (null != validationDate) {
                    trustValidator.isTrusted(encodedAttributeCertificates, certificateChain, validationDate);
                } else {
                    trustValidator.isTrusted(encodedAttributeCertificates, certificateChain);
                }
            } else {
                if (null != validationDate) {
                    trustValidator.isTrusted(certificateChain, validationDate);
                } else {
                    trustValidator.isTrusted(certificateChain);
                }
            }


        } catch (CertPathValidatorException e) {
            throw new ValidationFailedException(
                    Collections.singletonList(XKMSConstants.KEY_BINDING_REASON_ISSUER_TRUST_URI));
        }

    }
}
