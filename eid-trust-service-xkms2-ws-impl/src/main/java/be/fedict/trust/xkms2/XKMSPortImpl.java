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

package be.fedict.trust.xkms2;

import be.fedict.trust.CRLRevocationData;
import be.fedict.trust.OCSPRevocationData;
import be.fedict.trust.RevocationData;
import be.fedict.trust.client.jaxb.xades132.CRLValuesType;
import be.fedict.trust.client.jaxb.xades132.EncapsulatedPKIDataType;
import be.fedict.trust.client.jaxb.xades132.OCSPValuesType;
import be.fedict.trust.client.jaxb.xades132.RevocationValuesType;
import be.fedict.trust.client.jaxb.xkms.*;
import be.fedict.trust.client.jaxb.xmldsig.KeyInfoType;
import be.fedict.trust.client.jaxb.xmldsig.X509DataType;
import be.fedict.trust.service.TrustService;
import be.fedict.trust.service.ValidationResult;
import be.fedict.trust.service.exception.TrustDomainNotFoundException;
import be.fedict.trust.xkms.extensions.AttributeCertificateMessageExtensionType;
import be.fedict.trust.xkms.extensions.RevocationDataMessageExtensionType;
import be.fedict.trust.xkms.extensions.TSAMessageExtensionType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.tsp.TSPException;
import org.w3._2002._03.xkms.XKMSPortType;

import javax.ejb.EJB;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.*;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of XKMS2 Web Service JAX-WS Port.
 *
 * @author fcorneli
 */
@WebService(endpointInterface = "org.w3._2002._03.xkms.XKMSPortType")
@ServiceConsumer
@HandlerChain(file = "ws-handlers.xml")
public class XKMSPortImpl implements XKMSPortType {

    private static final Log LOG = LogFactory.getLog(XKMSPortImpl.class);

    private static final QName X509_CERT_QNAME = new QName(
            "http://www.w3.org/2000/09/xmldsig#", "X509Certificate");

    @EJB
    private TrustService trustService;

    @SuppressWarnings("unchecked")
    public ValidateResultType validate(ValidateRequestType body) {
        LOG.debug("validate");

        List<X509Certificate> certificateChain = new LinkedList<X509Certificate>();
        String trustDomain = null;
        boolean returnRevocationData = false;
        Date validationDate = null;
        List<byte[]> ocspResponses = new LinkedList<byte[]>();
        List<byte[]> crls = new LinkedList<byte[]>();
        byte[] timestampToken = null;
        List<byte[]> attributeCertificates = new LinkedList<byte[]>();

        /*
           * Get certification chain from QueryKeyBinding
           */
        QueryKeyBindingType queryKeyBinding = body.getQueryKeyBinding();
        KeyInfoType keyInfo = queryKeyBinding.getKeyInfo();
        List<Object> keyInfoContent = keyInfo.getContent();
        for (Object keyInfoObject : keyInfoContent) {
            JAXBElement<?> keyInfoElement = (JAXBElement<?>) keyInfoObject;
            Object elementValue = keyInfoElement.getValue();
            if (elementValue instanceof X509DataType) {
                X509DataType x509Data = (X509DataType) elementValue;
                List<Object> x509DataContent = x509Data
                        .getX509IssuerSerialOrX509SKIOrX509SubjectName();
                for (Object x509DataObject : x509DataContent) {
                    if (!(x509DataObject instanceof JAXBElement)) {
                        continue;
                    }
                    JAXBElement<?> x509DataElement = (JAXBElement<?>) x509DataObject;
                    if (!X509_CERT_QNAME.equals(x509DataElement
                            .getName())) {
                        continue;
                    }
                    byte[] x509DataValue = (byte[]) x509DataElement.getValue();
                    try {
                        X509Certificate certificate = getCertificate(x509DataValue);
                        certificateChain.add(certificate);
                    } catch (CertificateException e) {
                        return createResultResponse(ResultMajorCode.SENDER,
                                ResultMinorCode.MESSAGE_NOT_SUPPORTED);
                    }
                }
            }
        }

        /*
           * Get optional trust domain name from UseKeyWith
           */
        if (body.getQueryKeyBinding().getUseKeyWith().size() > 0) {
            for (UseKeyWithType useKeyWith : body.getQueryKeyBinding()
                    .getUseKeyWith()) {
                if (useKeyWith.getApplication().equals(
                        XKMSConstants.TRUST_DOMAIN_APPLICATION_URI)) {
                    trustDomain = useKeyWith.getIdentifier();
                    LOG.debug("validate against trust domain " + trustDomain);
                }
            }
        }

        /*
           * Get optional returning of used revocation data from RespondWith
           */
        if (body.getRespondWith().contains(
                XKMSConstants.RETURN_REVOCATION_DATA_URI)) {
            LOG.debug("will return used revocation data...");
            returnRevocationData = true;
        }

        /*
           * Get optional validation date from TimeInstant field for historical
           * validation
           */
        if (null != body.getQueryKeyBinding().getTimeInstant()) {
            validationDate = getDate(body.getQueryKeyBinding().getTimeInstant()
                    .getTime());
        }

        /*
           * Check for message extensions, these can be:
           *
           * RevocatioDataMessageExtension: historical validation, contains to be
           * used OCSP/CRL data
           *
           * TSAMessageExtension: TSA validation, contains encoded XAdES timestamp
           * token
           *
           * AttributeCertificateMessageExtension: Attribute certificate
           * validation, contains XAdES CertifiedRole element containing the
           * encoded Attribute certificate
           */
        for (MessageExtensionAbstractType messageExtension : body
                .getMessageExtension()) {
            if (messageExtension instanceof RevocationDataMessageExtensionType) {

                RevocationDataMessageExtensionType revocationDataMessageExtension = (RevocationDataMessageExtensionType) messageExtension;
                parseRevocationDataExtension(revocationDataMessageExtension,
                        ocspResponses, crls);

            } else if (messageExtension instanceof TSAMessageExtensionType) {

                TSAMessageExtensionType tsaMessageExtension = (TSAMessageExtensionType) messageExtension;
                timestampToken = parseTSAExtension(tsaMessageExtension);

            } else if (messageExtension instanceof AttributeCertificateMessageExtensionType) {

                AttributeCertificateMessageExtensionType attributeCertificateMessageExtension = (AttributeCertificateMessageExtensionType) messageExtension;
                parseAttributeCertificateExtension(
                        attributeCertificateMessageExtension,
                        attributeCertificates);

            } else {
                LOG.error("invalid message extension: "
                        + messageExtension.getClass().toString());
                return createResultResponse(ResultMajorCode.SENDER,
                        ResultMinorCode.MESSAGE_NOT_SUPPORTED);
            }
        }

        /*
           * Check gathered data
           */
        if (null != validationDate && ocspResponses.isEmpty() && crls.isEmpty()) {

            LOG
                    .error("Historical validation requested but no revocation data provided");
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.MESSAGE_NOT_SUPPORTED);

        } else if (null != timestampToken && !certificateChain.isEmpty()) {

            LOG
                    .error("Cannot both add a timestamp token and a seperate certificate chain");
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.MESSAGE_NOT_SUPPORTED);
        } else if (!attributeCertificates.isEmpty()
                && certificateChain.isEmpty()) {

            LOG
                    .error("No certificate chain provided for the attribute certificates");
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.MESSAGE_NOT_SUPPORTED);

        } else if (body.getMessageExtension().size() > 1) {

            LOG.error("Only 1 message extension at a time is supported");
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.MESSAGE_NOT_SUPPORTED);
        }

        /*
           * Validate!
           */
        ValidationResult validationResult;
        try {
            if (null != timestampToken) {
                validationResult = this.trustService.validateTimestamp(
                        trustDomain, timestampToken, returnRevocationData);
            } else if (!attributeCertificates.isEmpty()) {
                validationResult = this.trustService
                        .validateAttributeCertificates(trustDomain,
                                attributeCertificates, certificateChain,
                                returnRevocationData);
            } else if (null == validationDate) {
                validationResult = this.trustService.validate(trustDomain,
                        certificateChain, returnRevocationData);
            } else {
                validationResult = this.trustService.validate(trustDomain,
                        certificateChain, validationDate, ocspResponses, crls);
            }
        } catch (TrustDomainNotFoundException e) {
            LOG.error("invalid trust domain");
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.TRUST_DOMAIN_NOT_FOUND);
        } catch (CRLException e) {
            LOG.error("CRLException: " + e.getMessage(), e);
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.MESSAGE_NOT_SUPPORTED);
        } catch (IOException e) {
            LOG.error("IOException: " + e.getMessage(), e);
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.MESSAGE_NOT_SUPPORTED);
        } catch (CertificateException e) {
            LOG.error("CertificateException: " + e.getMessage(), e);
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.MESSAGE_NOT_SUPPORTED);
        } catch (NoSuchProviderException e) {
            LOG.error("NoSuchProviderException: " + e.getMessage(), e);
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.MESSAGE_NOT_SUPPORTED);
        } catch (TSPException e) {
            LOG.error("TSPException: " + e.getMessage(), e);
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.MESSAGE_NOT_SUPPORTED);
        } catch (CMSException e) {
            LOG.error("CMSException: " + e.getMessage(), e);
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.MESSAGE_NOT_SUPPORTED);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("NoSuchAlgorithmException: " + e.getMessage(), e);
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.MESSAGE_NOT_SUPPORTED);
        } catch (CertStoreException e) {
            LOG.error("CertStoreException: " + e.getMessage(), e);
            return createResultResponse(ResultMajorCode.SENDER,
                    ResultMinorCode.MESSAGE_NOT_SUPPORTED);
        }

        /*
           * Create validation result response
           */
        ValidateResultType validateResult = createResultResponse(
                ResultMajorCode.SUCCESS, null);

        ObjectFactory objectFactory = new ObjectFactory();
        List<KeyBindingType> keyBindings = validateResult.getKeyBinding();
        KeyBindingType keyBinding = objectFactory.createKeyBindingType();
        keyBindings.add(keyBinding);
        StatusType status = objectFactory.createStatusType();
        keyBinding.setStatus(status);
        String statusValue;
        if (validationResult.isValid()) {
            statusValue = XKMSConstants.KEY_BINDING_STATUS_VALID_URI;
        } else {
            statusValue = XKMSConstants.KEY_BINDING_STATUS_INVALID_URI;
        }
        status.setStatusValue(statusValue);

        /*
           * Add InvalidReason URI's
           */
        if (!validationResult.isValid()) {
            switch (validationResult.getReason()) {
                case INVALID_TRUST: {
                    status.getInvalidReason().add(
                            XKMSConstants.KEY_BINDING_REASON_ISSUER_TRUST_URI);
                    break;
                }
                case INVALID_REVOCATION_STATUS: {
                    status.getInvalidReason().add(
                            XKMSConstants.KEY_BINDING_REASON_REVOCATION_STATUS_URI);
                    break;
                }
                case INVALID_SIGNATURE: {
                    status.getInvalidReason().add(
                            XKMSConstants.KEY_BINDING_REASON_SIGNATURE_URI);
                    break;
                }
                case INVALID_VALIDITY_INTERVAL: {
                    status.getInvalidReason().add(
                            XKMSConstants.KEY_BINDING_REASON_VALIDITY_INTERVAL_URI);
                    break;
                }
            }
        }

        /*
           * Add used revocation data if requested
           */
        if (returnRevocationData) {
            addRevocationData(validateResult, validationResult
                    .getRevocationData());
        }

        return validateResult;
    }

    /*
     * Parse the {@link RevocationDataMessageExtensionType} for encoded OCSP
     * responses and/or encoded CRLs
     */

    private void parseRevocationDataExtension(
            RevocationDataMessageExtensionType revocationDataMessageExtension,
            List<byte[]> ocspResponses, List<byte[]> crls) {

        if (null == revocationDataMessageExtension.getRevocationValues()) {
            return;
        }

        if (null != revocationDataMessageExtension.getRevocationValues()
                .getOCSPValues()) {
            for (EncapsulatedPKIDataType ocspValue : revocationDataMessageExtension
                    .getRevocationValues().getOCSPValues()
                    .getEncapsulatedOCSPValue()) {
                ocspResponses.add(ocspValue.getValue());
            }
        }
        if (null != revocationDataMessageExtension.getRevocationValues()
                .getCRLValues()) {
            for (EncapsulatedPKIDataType crlValue : revocationDataMessageExtension
                    .getRevocationValues().getCRLValues()
                    .getEncapsulatedCRLValue()) {
                crls.add(crlValue.getValue());
            }
        }
    }

    /*
     * Parse the {@link TSAMessageExtensionType} and return an encoded timestamp
     * token or <code>null</code> if none found.
     */

    private byte[] parseTSAExtension(TSAMessageExtensionType tsaMessageExtension) {

        if (null == tsaMessageExtension.getEncapsulatedTimeStamp()) {
            return null;
        }
        return tsaMessageExtension.getEncapsulatedTimeStamp().getValue();
    }

    /*
     * Parse the {@link AttributeCertificateMessageExtensionType} and return an
     * encoded attribute certificate or <code>null</code> if none found
     */

    private void parseAttributeCertificateExtension(
            AttributeCertificateMessageExtensionType attributeCertificateMessageExtension,
            List<byte[]> attributeCertificates) {

        if (null == attributeCertificateMessageExtension.getCertifiedRoles()) {
            return;
        }

        for (EncapsulatedPKIDataType attributeCertificate : attributeCertificateMessageExtension
                .getCertifiedRoles().getCertifiedRole()) {
            attributeCertificates.add(attributeCertificate.getValue());
        }
    }

    private ValidateResultType createResultResponse(
            ResultMajorCode resultMajorCode, ResultMinorCode resultMinorCode) {

        ObjectFactory objectFactory = new ObjectFactory();
        ValidateResultType validateResult = objectFactory
                .createValidateResultType();
        if (null != resultMajorCode)
            validateResult.setResultMajor(resultMajorCode.getErrorCode());
        else
            validateResult.setResultMajor(ResultMajorCode.SUCCESS
                    .getErrorCode());
        if (null != resultMinorCode)
            validateResult.setResultMinor(resultMinorCode.getErrorCode());

        return validateResult;
    }

    private X509Certificate getCertificate(byte[] encodedCertificate)
            throws CertificateException {

        CertificateFactory certificateFactory = CertificateFactory
                .getInstance("X.509");
        return (X509Certificate) certificateFactory
                .generateCertificate(new ByteArrayInputStream(
                        encodedCertificate));
    }

    private Date getDate(XMLGregorianCalendar xmlCalendar) {

        GregorianCalendar calendar = new GregorianCalendar(xmlCalendar
                .getYear(), xmlCalendar.getMonth() - 1, xmlCalendar.getDay(), //
                xmlCalendar.getHour(), xmlCalendar.getMinute(), xmlCalendar
                        .getSecond());
        calendar.setTimeZone(xmlCalendar.getTimeZone(0));
        return calendar.getTime();
    }

    /*
     * Add the used revocation data if requested to the validation response
     */

    private void addRevocationData(ValidateResultType validateResult,
                                   RevocationData revocationData) {

        be.fedict.trust.xkms.extensions.ObjectFactory extensionsObjectFactory = new be.fedict.trust.xkms.extensions.ObjectFactory();
        RevocationDataMessageExtensionType revocationDataMessageExtension = extensionsObjectFactory
                .createRevocationDataMessageExtensionType();
        be.fedict.trust.client.jaxb.xades132.ObjectFactory xadesObjectFactory = new be.fedict.trust.client.jaxb.xades132.ObjectFactory();
        RevocationValuesType revocationValues = xadesObjectFactory
                .createRevocationValuesType();

        /*
         * Add OCSP responses
         */
        OCSPValuesType ocspValues = xadesObjectFactory.createOCSPValuesType();
        for (OCSPRevocationData ocspRevocationData : revocationData
                .getOcspRevocationData()) {
            EncapsulatedPKIDataType encapsulatedPKIData = xadesObjectFactory
                    .createEncapsulatedPKIDataType();
            encapsulatedPKIData.setValue(ocspRevocationData.getData());
            ocspValues.getEncapsulatedOCSPValue().add(encapsulatedPKIData);
        }
        revocationValues.setOCSPValues(ocspValues);

        /*
         * Add CRL's
         */
        CRLValuesType crlValues = xadesObjectFactory.createCRLValuesType();
        for (CRLRevocationData crlRevocationData : revocationData
                .getCrlRevocationData()) {
            EncapsulatedPKIDataType encapsulatedPKIData = xadesObjectFactory
                    .createEncapsulatedPKIDataType();
            encapsulatedPKIData.setValue(crlRevocationData.getData());
            crlValues.getEncapsulatedCRLValue().add(encapsulatedPKIData);
        }
        revocationValues.setCRLValues(crlValues);

        revocationDataMessageExtension.setRevocationValues(revocationValues);
        validateResult.getMessageExtension()
                .add(revocationDataMessageExtension);
    }
}
