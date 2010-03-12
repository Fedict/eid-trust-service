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

import java.io.IOException;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.util.encoders.Base64;
import org.etsi.uri._01903.v1_3.CRLValuesType;
import org.etsi.uri._01903.v1_3.EncapsulatedPKIDataType;
import org.etsi.uri._01903.v1_3.OCSPValuesType;
import org.etsi.uri._01903.v1_3.RevocationValuesType;
import org.w3._2000._09.xmldsig_.KeyInfoType;
import org.w3._2000._09.xmldsig_.X509DataType;
import org.w3._2002._03.xkms_.KeyBindingType;
import org.w3._2002._03.xkms_.MessageExtensionAbstractType;
import org.w3._2002._03.xkms_.ObjectFactory;
import org.w3._2002._03.xkms_.QueryKeyBindingType;
import org.w3._2002._03.xkms_.StatusType;
import org.w3._2002._03.xkms_.TimeInstantType;
import org.w3._2002._03.xkms_.UseKeyWithType;
import org.w3._2002._03.xkms_.ValidateRequestType;
import org.w3._2002._03.xkms_.ValidateResultType;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;
import org.w3._2002._03.xkms_wsdl.XKMSService;

import be.fedict.trust.client.exception.RevocationDataNotFoundException;
import be.fedict.trust.client.exception.TrustDomainNotFoundException;
import be.fedict.trust.xkms.extensions.RevocationDataMessageExtensionType;
import be.fedict.trust.xkms2.ResultMajorCode;
import be.fedict.trust.xkms2.ResultMinorCode;
import be.fedict.trust.xkms2.XKMSConstants;
import be.fedict.trust.xkms2.XKMSServiceFactory;

/**
 * Client component for the eID Trust Service XKMS2 web service.
 * 
 * @author fcorneli
 * 
 */
public class XKMS2Client {

	private static final Log LOG = LogFactory.getLog(XKMS2Client.class);

	private final XKMSPortType port;

	private final String location;

	private RevocationValuesType revocationValues;

	private List<String> reasonURIs;

	/**
	 * Main constructor
	 * 
	 * @param location
	 *            the location (host:port) of the XKMS2 web service
	 */
	public XKMS2Client(String location) {

		this.reasonURIs = new LinkedList<String>();

		XKMSService xkmsService = XKMSServiceFactory.getInstance();
		port = xkmsService.getXKMSPort();
		this.location = MessageFormat.format("{0}/eid-trust-service-ws/xkms2",
				location);

		setEndpointAddress();
	}

	private void setEndpointAddress() {

		BindingProvider bindingProvider = (BindingProvider) port;
		bindingProvider.getRequestContext().put(
				BindingProvider.ENDPOINT_ADDRESS_PROPERTY, location);
	}

	/**
	 * Registers the logging SOAP handler on the given JAX-WS port component.
	 * 
	 * @param port
	 */
	protected void registerLoggerHandler(Object port) {

		BindingProvider bindingProvider = (BindingProvider) port;

		Binding binding = bindingProvider.getBinding();
		@SuppressWarnings("unchecked")
		List<Handler> handlerChain = binding.getHandlerChain();
		handlerChain.add(new LoggingSoapHandler());
		binding.setHandlerChain(handlerChain);
	}

	/**
	 * Validate the specified certificate chain against the default trust domain
	 * configured at the trust service we are connecting to.
	 * 
	 * @param certificateChain
	 * 
	 * @throws CertificateEncodingException
	 * @throws TrustDomainNotFoundException
	 * @throws RevocationDataNotFoundException
	 */
	public boolean validate(List<X509Certificate> certificateChain)
			throws CertificateEncodingException, TrustDomainNotFoundException,
			RevocationDataNotFoundException {

		return validate(null, certificateChain);
	}

	/**
	 * Validate the specified certificate chain against the default trust domain
	 * configured at the trust service we are connecting to.
	 * 
	 * The used revocation data can be retrieved using
	 * {@link #getRevocationValues()}.
	 * 
	 * @param certificateChain
	 * @param returnRevocationData
	 *            whether or not the used revocation data should be returned.
	 * 
	 * @throws CertificateEncodingException
	 * @throws TrustDomainNotFoundException
	 * @throws RevocationDataNotFoundException
	 */
	public boolean validate(List<X509Certificate> certificateChain,
			boolean returnRevocationData) throws CertificateEncodingException,
			TrustDomainNotFoundException, RevocationDataNotFoundException {

		return validate(null, certificateChain, returnRevocationData);
	}

	/**
	 * Validate the specified certificate chain against the specified trust
	 * domain.
	 * 
	 * @param trustDomain
	 * @param certificateChain
	 * 
	 * @throws CertificateEncodingException
	 * @throws TrustDomainNotFoundException
	 * @throws RevocationDataNotFoundException
	 */
	public boolean validate(String trustDomain,
			List<X509Certificate> certificateChain)
			throws CertificateEncodingException, TrustDomainNotFoundException,
			RevocationDataNotFoundException {

		return validate(trustDomain, certificateChain, false);
	}

	/**
	 * Validate the specified certificate chain against the specified trust
	 * domain using historical validation using the specified revocation data.
	 * 
	 * @param trustDomain
	 * @param certificateChain
	 * @param validationDate
	 * @param ocspResponses
	 * @param crls
	 * 
	 * @throws RevocationDataNotFoundException
	 * @throws TrustDomainNotFoundException
	 * @throws CertificateEncodingException
	 */
	public boolean validate(String trustDomain,
			List<X509Certificate> certificateChain, Date validationDate,
			List<OCSPResp> ocspResponses, List<X509CRL> crls)
			throws CertificateEncodingException, TrustDomainNotFoundException,
			RevocationDataNotFoundException {

		return validate(trustDomain, certificateChain, false, validationDate,
				ocspResponses, crls, null);
	}

	/**
	 * Validate the specified certificate chain against the specified trust
	 * domain using historical validation using the specified revocation data.
	 * 
	 * @param trustDomain
	 * @param certificateChain
	 * @param validationDate
	 * @param ocspResponses
	 * @param crls
	 * 
	 * @throws RevocationDataNotFoundException
	 * @throws TrustDomainNotFoundException
	 * @throws CertificateEncodingException
	 */
	public boolean validate(String trustDomain,
			List<X509Certificate> certificateChain, Date validationDate,
			RevocationValuesType revocationValues)
			throws CertificateEncodingException, TrustDomainNotFoundException,
			RevocationDataNotFoundException {

		return validate(trustDomain, certificateChain, false, validationDate,
				null, null, revocationValues);
	}

	/**
	 * Validate the specified certificate chain against the specified trust
	 * domain.
	 * 
	 * The used revocation data can be retrieved using
	 * {@link #getRevocationValues()}.
	 * 
	 * @param trustDomain
	 * @param certificateChain
	 * @param returnRevocationData
	 *            whether or not the used revocation data should be returned.
	 * 
	 * @throws CertificateEncodingException
	 * @throws TrustDomainNotFoundException
	 * @throws RevocationDataNotFoundException
	 */
	public boolean validate(String trustDomain,
			List<X509Certificate> certificateChain, boolean returnRevocationData)
			throws CertificateEncodingException, TrustDomainNotFoundException,
			RevocationDataNotFoundException {

		return validate(trustDomain, certificateChain, returnRevocationData,
				null, null, null, null);
	}

	private boolean validate(String trustDomain,
			List<X509Certificate> authnCertificateChain,
			boolean returnRevocationData, Date validationDate,
			List<OCSPResp> ocspResponses, List<X509CRL> crls,
			RevocationValuesType revocationValues)
			throws CertificateEncodingException, TrustDomainNotFoundException,
			RevocationDataNotFoundException {

		LOG.debug("validate: "
				+ authnCertificateChain.get(0).getSubjectX500Principal());

		ObjectFactory objectFactory = new ObjectFactory();
		org.w3._2000._09.xmldsig_.ObjectFactory xmldsigObjectFactory = new org.w3._2000._09.xmldsig_.ObjectFactory();

		ValidateRequestType validateRequest = objectFactory
				.createValidateRequestType();
		QueryKeyBindingType queryKeyBinding = objectFactory
				.createQueryKeyBindingType();
		KeyInfoType keyInfo = xmldsigObjectFactory.createKeyInfoType();
		queryKeyBinding.setKeyInfo(keyInfo);
		X509DataType x509Data = xmldsigObjectFactory.createX509DataType();
		for (X509Certificate certificate : authnCertificateChain) {
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

		// TODO: WS trust via unilateral TLS trust model based on public key

		ValidateResultType validateResult = port.validate(validateRequest);

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

		List<KeyBindingType> keyBindings = validateResult.getKeyBinding();
		for (KeyBindingType keyBinding : keyBindings) {
			// TODO better result verification
			StatusType status = keyBinding.getStatus();
			String statusValue = status.getStatusValue();
			LOG.debug("status: " + statusValue);
			if (XKMSConstants.KEY_BINDING_STATUS_VALID_URI.equals(statusValue)) {
				return true;
			}
			for (String invalidReason : status.getInvalidReason()) {
				this.reasonURIs.add(invalidReason);
			}
		}
		return false;
	}

	/**
	 * Add revocation data either from list of {@link OCSPResp} objects and
	 * {@link X509CRL} objects or from specified {@link RevocationValuesType}.
	 * 
	 * @param validateRequest
	 * @param ocspResponses
	 * @param crls
	 * @param revocationData
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
			org.etsi.uri._01903.v1_3.ObjectFactory xadesObjectFactory = new org.etsi.uri._01903.v1_3.ObjectFactory();
			RevocationValuesType revocationValues = xadesObjectFactory
					.createRevocationValuesType();

			// OCSP
			OCSPValuesType ocspValues = xadesObjectFactory
					.createOCSPValuesType();
			for (OCSPResp ocspResponse : ocspResponses) {
				EncapsulatedPKIDataType ocspValue = xadesObjectFactory
						.createEncapsulatedPKIDataType();
				try {
					ocspValue
							.setValue(Base64.encode(ocspResponse.getEncoded()));
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
					crlValue.setValue(Base64.encode(crl.getEncoded()));
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
	 * Checks the ResultMajor and ResultMinor code.
	 * 
	 * @param validateResult
	 * @throws TrustDomainNotFoundException
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
	 * Returns the list of XKMS2 reason URI's in case validation has failed.
	 * 
	 * {@link http://www.w3.org/TR/xkms2/#XKMS_2_0_Section_5_1}
	 */
	public List<String> getReasonURIs() {

		return this.reasonURIs;
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
