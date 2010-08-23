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

import be.fedict.trust.client.exception.RevocationDataNotFoundException;
import be.fedict.trust.client.exception.TrustDomainNotFoundException;
import be.fedict.trust.client.exception.ValidationFailedException;
import be.fedict.trust.client.jaxb.xades.v1_3.*;
import be.fedict.trust.xkms.extensions.AttributeCertificateMessageExtensionType;
import be.fedict.trust.xkms.extensions.RevocationDataMessageExtensionType;
import be.fedict.trust.xkms.extensions.TSAMessageExtensionType;
import be.fedict.trust.xkms2.*;
import com.sun.xml.ws.developer.JAXWSProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.x509.X509V2AttributeCertificate;
import org.w3._2000._09.xmldsig.KeyInfoType;
import org.w3._2000._09.xmldsig.X509DataType;
import org.w3._2002._03.xkms.*;
import org.w3._2002._03.xkms.ObjectFactory;
import sun.security.timestamp.TimestampToken;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import java.io.IOException;
import java.security.*;
import java.security.cert.*;
import java.util.*;

/**
 * Client component for the eID Trust Service XKMS2 web service.
 *
 * @author fcorneli
 */
public class XKMS2Client {

    private static final Log LOG = LogFactory.getLog(XKMS2Client.class);

    private final XKMSPortType port;

    private RevocationValuesType revocationValues;

    private WSSecurityClientHandler wsSecurityClientHandler;

    private List<String> invalidReasonURIs;

    /**
     * Main constructor
     *
     * @param location location ( complete path ) of the XKMS2 web service
     */
    public XKMS2Client(String location) {

        this.invalidReasonURIs = new LinkedList<String>();

        XKMSService xkmsService = XKMSServiceFactory.getInstance();
        this.port = xkmsService.getXKMSPort();

        registeredWSSecurityHandler(this.port);
        registerLoggerHandler(this.port);
        setEndpointAddress(location);
    }

    /**
     * Proxy configuration setting ( both http as https ).
     */
    public void setProxy(String proxyHost, int proxyPort) {

        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", Integer.toString(proxyPort));
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", Integer.toString(proxyPort));
    }

    /**
     * Set the optional server {@link X509Certificate}. If specified and the
     * trust service has message signing configured, the incoming
     * {@link X509Certificate} will be checked against the specified
     * {@link X509Certificate}.
     *
     * @param serverCertificate the server X509 certificate.
     */
    public void setServerCertificate(X509Certificate serverCertificate) {

        this.wsSecurityClientHandler.setServerCertificate(serverCertificate);
    }

    /**
     * Set the maximum offset of the WS-Security timestamp ( in ms ). If not
     * specified this will be defaulted to 5 minutes.
     */
    public void setMaxWSSecurityTimestampOffset(
            long maxWSSecurityTimestampOffset) {

        this.wsSecurityClientHandler
                .setMaxWSSecurityTimestampOffset(maxWSSecurityTimestampOffset);
    }

    /**
     * If set, unilateral TLS authentication will occurs, verifying the server
     * {@link X509Certificate} specified {@link PublicKey}.
     */
    public void setServicePublicKey(final PublicKey publicKey) {

        // Create TrustManager
        TrustManager[] trustManager = {new X509TrustManager() {

            public X509Certificate[] getAcceptedIssuers() {

                return null;
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {

                X509Certificate serverCertificate = chain[0];
                LOG.debug("server X509 subject: "
                        + serverCertificate.getSubjectX500Principal()
                        .toString());
                LOG.debug("authentication type: " + authType);
                if (null == publicKey)
                    return;

                try {
                    serverCertificate.verify(publicKey);
                    LOG.debug("valid server certificate");
                } catch (InvalidKeyException e) {
                    throw new CertificateException("Invalid Key");
                } catch (NoSuchAlgorithmException e) {
                    throw new CertificateException("No such algorithm");
                } catch (NoSuchProviderException e) {
                    throw new CertificateException("No such provider");
                } catch (SignatureException e) {
                    throw new CertificateException("Wrong signature");
                }
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {

                throw new CertificateException(
                        "this trust manager cannot be used as server-side trust manager");
            }
        }};

        // Create SSL Context
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            SecureRandom secureRandom = new SecureRandom();
            sslContext.init(null, trustManager, secureRandom);
            LOG.debug("SSL context provider: "
                    + sslContext.getProvider().getName());

            // Setup TrustManager for validation
            Map<String, Object> requestContext = ((BindingProvider) this.port)
                    .getRequestContext();
            requestContext.put(JAXWSProperties.SSL_SOCKET_FACTORY, sslContext
                    .getSocketFactory());

        } catch (KeyManagementException e) {
            String msg = "key management error: " + e.getMessage();
            LOG.error(msg, e);
            throw new RuntimeException(msg, e);
        } catch (NoSuchAlgorithmException e) {
            String msg = "TLS algo not present: " + e.getMessage();
            LOG.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private void setEndpointAddress(String location) {

        LOG.debug("ws location=" + location);
        BindingProvider bindingProvider = (BindingProvider) this.port;
        bindingProvider.getRequestContext().put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY, location);
    }

    /**
     * Registers the logging SOAP handler on the given JAX-WS port component.
     */
    protected void registerLoggerHandler(Object port) {

        BindingProvider bindingProvider = (BindingProvider) port;

        Binding binding = bindingProvider.getBinding();
        @SuppressWarnings("unchecked")
        List<Handler> handlerChain = binding.getHandlerChain();
        handlerChain.add(new LoggingSoapHandler());
        binding.setHandlerChain(handlerChain);
    }

    protected void registeredWSSecurityHandler(Object port) {

        BindingProvider bindingProvider = (BindingProvider) port;

        Binding binding = bindingProvider.getBinding();
        @SuppressWarnings("unchecked")
        List<Handler> handlerChain = binding.getHandlerChain();
        this.wsSecurityClientHandler = new WSSecurityClientHandler();
        handlerChain.add(this.wsSecurityClientHandler);
        binding.setHandlerChain(handlerChain);

    }

    /**
     * Validate the specified certificate chain against the default trust domain
     * configured at the trust service we are connecting to.
     */
    public void validate(List<X509Certificate> certificateChain)
            throws CertificateEncodingException, TrustDomainNotFoundException,
            RevocationDataNotFoundException, ValidationFailedException {

        validate(null, certificateChain);
    }

    /**
     * Validate the specified certificate chain against the default trust domain
     * configured at the trust service we are connecting to.
     * <p/>
     * The used revocation data can be retrieved using
     * {@link #getRevocationValues()}.
     *
     * @param returnRevocationData whether or not the used revocation data should be returned.
     */
    public void validate(List<X509Certificate> certificateChain,
                         boolean returnRevocationData) throws CertificateEncodingException,
            TrustDomainNotFoundException, RevocationDataNotFoundException,
            ValidationFailedException {

        validate(null, certificateChain, returnRevocationData);
    }

    /**
     * Validate the specified certificate chain against the specified trust
     * domain.
     */
    public void validate(String trustDomain,
                         List<X509Certificate> certificateChain)
            throws CertificateEncodingException, TrustDomainNotFoundException,
            RevocationDataNotFoundException, ValidationFailedException {

        validate(trustDomain, certificateChain, false);
    }

    /**
     * Validate the specified certificate chain against the specified trust
     * domain using historical validation using the specified revocation data.
     */
    public void validate(String trustDomain,
                         List<X509Certificate> certificateChain, Date validationDate,
                         List<OCSPResp> ocspResponses, List<X509CRL> crls)
            throws CertificateEncodingException, TrustDomainNotFoundException,
            RevocationDataNotFoundException, ValidationFailedException {

        validate(trustDomain, certificateChain, false, validationDate,
                ocspResponses, crls, null, null, null);
    }

    /**
     * Validate the specified certificate chain against the specified trust
     * domain using historical validation using the specified revocation data.
     */
    public void validate(String trustDomain,
                         List<X509Certificate> certificateChain, Date validationDate,
                         RevocationValuesType revocationValues)
            throws CertificateEncodingException, TrustDomainNotFoundException,
            RevocationDataNotFoundException, ValidationFailedException {

        validate(trustDomain, certificateChain, false, validationDate, null,
                null, revocationValues, null, null);
    }

    /**
     * Validate the specified certificate chain against the specified trust
     * domain.
     * <p/>
     * The used revocation data can be retrieved using
     * {@link #getRevocationValues()}.
     *
     * @param returnRevocationData whether or not the used revocation data should be returned.
     */
    public void validate(String trustDomain,
                         List<X509Certificate> certificateChain, boolean returnRevocationData)
            throws CertificateEncodingException, TrustDomainNotFoundException,
            RevocationDataNotFoundException, ValidationFailedException {

        validate(trustDomain, certificateChain, returnRevocationData, null,
                null, null, null, null, null);
    }

    /**
     * Validate the specified {@link TimestampToken} for the specified TSA trust
     * domain
     */
    public void validate(String trustDomain, TimeStampToken timeStampToken)
            throws TrustDomainNotFoundException, CertificateEncodingException,
            RevocationDataNotFoundException, ValidationFailedException {

        LOG.debug("validate timestamp token");
        validate(trustDomain, new LinkedList<X509Certificate>(), false, null,
                null, null, revocationValues, timeStampToken, null);
    }

    /**
     * Validate the specified {@link be.fedict.trust.client.jaxb.xades.v1_3.EncapsulatedPKIDataType} holding the
     * {@link X509V2AttributeCertificate}.
     *
     * @param certificateChain the certificate chain for the attribute certificate
     */
    public void validate(String trustDomain,
                         List<X509Certificate> certificateChain,
                         CertifiedRolesListType attributeCertificates)
            throws CertificateEncodingException, TrustDomainNotFoundException,
            RevocationDataNotFoundException, ValidationFailedException {

        LOG.debug("validate attribute certificate");
        validate(trustDomain, certificateChain, false, null, null, null,
                revocationValues, null, attributeCertificates);
    }

    protected void validate(String trustDomain,
                          List<X509Certificate> certificateChain,
                          boolean returnRevocationData, Date validationDate,
                          List<OCSPResp> ocspResponses, List<X509CRL> crls,
                          RevocationValuesType revocationValues,
                          TimeStampToken timeStampToken,
                          CertifiedRolesListType attributeCertificates)
            throws CertificateEncodingException, TrustDomainNotFoundException,
            RevocationDataNotFoundException, ValidationFailedException {

        LOG.debug("validate");

        ObjectFactory objectFactory = new ObjectFactory();
        org.w3._2000._09.xmldsig.ObjectFactory xmldsigObjectFactory = new org.w3._2000._09.xmldsig.ObjectFactory();

        ValidateRequestType validateRequest = objectFactory
                .createValidateRequestType();
        QueryKeyBindingType queryKeyBinding = objectFactory
                .createQueryKeyBindingType();
        KeyInfoType keyInfo = xmldsigObjectFactory.createKeyInfoType();
        queryKeyBinding.setKeyInfo(keyInfo);
        X509DataType x509Data = xmldsigObjectFactory.createX509DataType();
        for (X509Certificate certificate : certificateChain) {
            byte[] encodedCertificate = certificate.getEncoded();
            x509Data
                    .getX509IssuerSerialOrX509SKIOrX509SubjectName()
                    .add(
                            xmldsigObjectFactory
                                    .createX509DataTypeX509Certificate(encodedCertificate));
        }
        keyInfo.getContent().add(xmldsigObjectFactory.createX509Data(x509Data));
        validateRequest.setQueryKeyBinding(queryKeyBinding);

        /*
           * Set optional trust domain
           */
        if (null != trustDomain) {
            UseKeyWithType useKeyWith = objectFactory.createUseKeyWithType();
            useKeyWith
                    .setApplication(XKMSConstants.TRUST_DOMAIN_APPLICATION_URI);
            useKeyWith.setIdentifier(trustDomain);
            queryKeyBinding.getUseKeyWith().add(useKeyWith);
        }

        /*
           * Add timestamp token for TSA validation
           */

        if (null != timeStampToken) {
            addTimeStampToken(validateRequest, timeStampToken);
        }

        /*
           * Add attribute certificates
           */
        if (null != attributeCertificates) {
            addAttributeCertificates(validateRequest, attributeCertificates);
        }

        /*
           * Set if used revocation data should be returned or not
           */
        if (returnRevocationData) {
            validateRequest.getRespondWith().add(
                    XKMSConstants.RETURN_REVOCATION_DATA_URI);
        }

        /*
           * Historical validation, add the revocation data to the request
           */
        if (null != validationDate) {

            TimeInstantType timeInstant = objectFactory.createTimeInstantType();
            timeInstant.setTime(getXmlGregorianCalendar(validationDate));
            queryKeyBinding.setTimeInstant(timeInstant);

            addRevocationData(validateRequest, ocspResponses, crls,
                    revocationValues);

        }

        ValidateResultType validateResult = this.port.validate(validateRequest);

        if (null == validateResult) {
            throw new RuntimeException("missing ValidateResult element");
        }

        checkResponse(validateResult);

        // set the optionally requested revocation data
        if (returnRevocationData) {
            for (MessageExtensionAbstractType messageExtension : validateResult
                    .getMessageExtension()) {
                if (messageExtension instanceof RevocationDataMessageExtensionType) {
                    this.revocationValues = ((RevocationDataMessageExtensionType) messageExtension)
                            .getRevocationValues();
                }
            }
            if (null == this.revocationValues) {
                LOG.error("no revocation data found");
                throw new RevocationDataNotFoundException();
            }
        }

        // store reason URIs
        this.invalidReasonURIs.clear();
        List<KeyBindingType> keyBindings = validateResult.getKeyBinding();
        for (KeyBindingType keyBinding : keyBindings) {
            StatusType status = keyBinding.getStatus();
            String statusValue = status.getStatusValue();
            LOG.debug("status: " + statusValue);
            if (XKMSConstants.KEY_BINDING_STATUS_VALID_URI.equals(statusValue)) {
                return;
            }
            for (String invalidReason : status.getInvalidReason()) {
                this.invalidReasonURIs.add(invalidReason);
            }
            throw new ValidationFailedException(invalidReasonURIs);
        }
    }

    /**
     * Add revocation data either from list of {@link OCSPResp} objects and
     * {@link X509CRL} objects or from specified {@link RevocationValuesType}.
     */
    private void addRevocationData(ValidateRequestType validateRequest,
                                   List<OCSPResp> ocspResponses, List<X509CRL> crls,
                                   RevocationValuesType revocationData) {

        be.fedict.trust.xkms.extensions.ObjectFactory extensionsObjectFactory = new be.fedict.trust.xkms.extensions.ObjectFactory();
        RevocationDataMessageExtensionType revocationDataMessageExtension = extensionsObjectFactory
                .createRevocationDataMessageExtensionType();

        if (null != revocationData) {
            revocationDataMessageExtension.setRevocationValues(revocationData);
        } else {
            be.fedict.trust.client.jaxb.xades.v1_3.ObjectFactory xadesObjectFactory = new be.fedict.trust.client.jaxb.xades.v1_3.ObjectFactory();
            RevocationValuesType revocationValues = xadesObjectFactory
                    .createRevocationValuesType();

            // OCSP
            OCSPValuesType ocspValues = xadesObjectFactory
                    .createOCSPValuesType();
            for (OCSPResp ocspResponse : ocspResponses) {
                EncapsulatedPKIDataType ocspValue = xadesObjectFactory
                        .createEncapsulatedPKIDataType();
                try {
                    ocspValue.setValue(ocspResponse.getEncoded());
                } catch (IOException e) {
                    LOG.error("IOException: " + e.getMessage(), e);
                    throw new RuntimeException(e);
                }
                ocspValues.getEncapsulatedOCSPValue().add(ocspValue);
            }
            revocationValues.setOCSPValues(ocspValues);

            // CRL
            CRLValuesType crlValues = xadesObjectFactory.createCRLValuesType();
            for (X509CRL crl : crls) {
                EncapsulatedPKIDataType crlValue = xadesObjectFactory
                        .createEncapsulatedPKIDataType();
                try {
                    crlValue.setValue(crl.getEncoded());
                } catch (CRLException e) {
                    LOG.error("CRLException: " + e.getMessage(), e);
                    throw new RuntimeException(e);
                }
                crlValues.getEncapsulatedCRLValue().add(crlValue);
            }
            revocationValues.setCRLValues(crlValues);
            revocationDataMessageExtension
                    .setRevocationValues(revocationValues);
        }

        validateRequest.getMessageExtension().add(
                revocationDataMessageExtension);
    }

    /**
     * Add the specified {@link TimeStampToken} to the
     * {@link ValidateRequestType}.
     */
    private void addTimeStampToken(ValidateRequestType validateRequest,
                                   TimeStampToken timeStampToken) {

        be.fedict.trust.xkms.extensions.ObjectFactory extensionsObjectFactory = new be.fedict.trust.xkms.extensions.ObjectFactory();
        be.fedict.trust.client.jaxb.xades.v1_3.ObjectFactory xadesObjectFactory = new be.fedict.trust.client.jaxb.xades.v1_3.ObjectFactory();

        TSAMessageExtensionType tsaMessageExtension = extensionsObjectFactory
                .createTSAMessageExtensionType();
        EncapsulatedPKIDataType timeStampTokenValue = xadesObjectFactory
                .createEncapsulatedPKIDataType();
        try {
            timeStampTokenValue.setValue(timeStampToken.getEncoded());
        } catch (IOException e) {
            LOG.error("Failed to get encoded timestamp token", e);
            throw new RuntimeException(e);
        }
        tsaMessageExtension.setEncapsulatedTimeStamp(timeStampTokenValue);
        validateRequest.getMessageExtension().add(tsaMessageExtension);
    }

    /**
     * Add the specified {@link EncapsulatedPKIDataType} holding the attribute
     * certificate to the {@link ValidateRequestType}.
     */
    private void addAttributeCertificates(ValidateRequestType validateRequest,
                                          CertifiedRolesListType attributeCertificates) {

        be.fedict.trust.xkms.extensions.ObjectFactory extensionsObjectFactory = new be.fedict.trust.xkms.extensions.ObjectFactory();

        AttributeCertificateMessageExtensionType attributeCertificateMessageExtension = extensionsObjectFactory
                .createAttributeCertificateMessageExtensionType();
        attributeCertificateMessageExtension
                .setCertifiedRoles(attributeCertificates);
        validateRequest.getMessageExtension().add(
                attributeCertificateMessageExtension);
    }

    /**
     * Checks the ResultMajor and ResultMinor code.
     */
    private void checkResponse(ValidateResultType validateResult)
            throws TrustDomainNotFoundException {

        if (!validateResult.getResultMajor().equals(
                ResultMajorCode.SUCCESS.getErrorCode())) {

            if (validateResult.getResultMinor().equals(
                    ResultMinorCode.TRUST_DOMAIN_NOT_FOUND.getErrorCode())) {
                throw new TrustDomainNotFoundException();
            }

        }
    }

    /**
     * Return the optionally filled {@link RevocationValuesType}. Returns
     * <code>null</code> if this was not specified.
     */
    public RevocationValuesType getRevocationValues() {

        return this.revocationValues;
    }

    /**
     * Returns the XKMS v2.0 reason URIs for the failed validation.
     *
     * @see <a href="http://www.w3.org/TR/xkms2/#XKMS_2_0_Section_5_1">XKMS
     *      2.0</a>
     */
    public List<String> getInvalidReasons() {

        return this.invalidReasonURIs;
    }

    private XMLGregorianCalendar getXmlGregorianCalendar(Date date) {

        try {
            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();

            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTime(date);
            XMLGregorianCalendar currentXmlGregorianCalendar = datatypeFactory
                    .newXMLGregorianCalendar(gregorianCalendar);
            return currentXmlGregorianCalendar;

        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException("datatype config error");
        }
    }

}
