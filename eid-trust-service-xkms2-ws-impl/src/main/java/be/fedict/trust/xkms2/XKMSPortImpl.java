/*
 * eID Trust Service Project.
 * Copyright (C) 2009 FedICT.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.jws.WebService;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

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
import org.w3._2002._03.xkms_.UseKeyWithType;
import org.w3._2002._03.xkms_.ValidateRequestType;
import org.w3._2002._03.xkms_.ValidateResultType;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

import sun.security.x509.X509CRLImpl;
import be.fedict.trust.CRLRevocationData;
import be.fedict.trust.OCSPRevocationData;
import be.fedict.trust.RevocationData;
import be.fedict.trust.service.TrustService;
import be.fedict.trust.service.ValidationResult;
import be.fedict.trust.service.exception.TrustDomainNotFoundException;
import be.fedict.trust.xkms.extensions.RevocationDataMessageExtensionType;

/**
 * Implementation of XKMS2 Web Service JAX-WS Port.
 * 
 * @author fcorneli
 * 
 */
@WebService(endpointInterface = "org.w3._2002._03.xkms_wsdl.XKMSPortType")
@ServiceConsumer
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

		// parse the request
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
					if (false == x509DataObject instanceof JAXBElement) {
						continue;
					}
					JAXBElement<?> x509DataElement = (JAXBElement<?>) x509DataObject;
					if (false == X509_CERT_QNAME.equals(x509DataElement
							.getName())) {
						continue;
					}
					byte[] x509DataValue = (byte[]) x509DataElement.getValue();
					try {
						X509Certificate certificate = getCertificate(x509DataValue);
						certificateChain.add(certificate);
					} catch (CertificateException e) {
						// TODO: proper error handling according to XKMS2 spec
						throw new RuntimeException("X509 encoding error");
					}
				}
			}
		}

		// look for a trust domain message extension
		String trustDomain = null;
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

		// look if revocation data should be returned
		boolean returnRevocationData = false;
		if (body.getRespondWith().contains(
				XKMSConstants.RETURN_REVOCATION_DATA_URI)) {
			LOG.debug("will return used revocation data...");
			returnRevocationData = true;
		}

		// look if historical validation is active
		Date validationDate = null;
		List<OCSPResp> ocspResponses = new LinkedList<OCSPResp>();
		List<X509CRL> crls = new LinkedList<X509CRL>();
		if (null != body.getQueryKeyBinding().getTimeInstant()) {
			try {
				validationDate = getDate(body.getQueryKeyBinding()
						.getTimeInstant().getTime());
				for (MessageExtensionAbstractType messageExtension : body
						.getMessageExtension()) {
					if (messageExtension instanceof RevocationDataMessageExtensionType) {
						RevocationDataMessageExtensionType revocationDataMessageExtension = (RevocationDataMessageExtensionType) messageExtension;
						if (null == revocationDataMessageExtension
								.getRevocationValues()) {
							LOG.error("missing revocation values");
							return createResultResponse(ResultMajorCode.SENDER,
									ResultMinorCode.INCOMPLETE);
						}
						if (null != revocationDataMessageExtension
								.getRevocationValues().getOCSPValues()) {
							for (EncapsulatedPKIDataType ocspValue : revocationDataMessageExtension
									.getRevocationValues().getOCSPValues()
									.getEncapsulatedOCSPValue()) {
								OCSPResp ocspResponse = new OCSPResp(Base64
										.decode(ocspValue.getValue()));
								ocspResponses.add(ocspResponse);
							}
						}
						if (null != revocationDataMessageExtension
								.getRevocationValues().getCRLValues()) {
							for (EncapsulatedPKIDataType crlValue : revocationDataMessageExtension
									.getRevocationValues().getCRLValues()
									.getEncapsulatedCRLValue()) {
								X509CRL crl = new X509CRLImpl(Base64
										.decode(crlValue.getValue()));
								crls.add(crl);
							}
						}
					} else {
						LOG.error("invalid message extension: "
								+ messageExtension.getClass().toString());
						return createResultResponse(ResultMajorCode.SENDER,
								ResultMinorCode.MESSAGE_NOT_SUPPORTED);
					}
				}
			} catch (CRLException e) {
				LOG.error("CRLException: " + e.getMessage(), e);
				return createResultResponse(ResultMajorCode.SENDER,
						ResultMinorCode.MESSAGE_NOT_SUPPORTED);
			} catch (IOException e) {
				LOG.error("IOException: " + e.getMessage(), e);
				return createResultResponse(ResultMajorCode.SENDER,
						ResultMinorCode.MESSAGE_NOT_SUPPORTED);
			}
		}

		ValidationResult validationResult;
		try {
			if (null == validationDate) {
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
		}

		// return the result
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
			statusValue = "http://www.w3.org/2002/03/xkms#Valid";
		} else {
			statusValue = "http://www.w3.org/2002/03/xkms#Invalid";
		}
		status.setStatusValue(statusValue);

		// optionally append used revocation data if specified
		if (returnRevocationData) {
			addRevocationData(validateResult, validationResult
					.getRevocationData());
		}

		return validateResult;
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
		X509Certificate certificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(
						encodedCertificate));
		return certificate;
	}

	private Date getDate(XMLGregorianCalendar xmlCalendar) {

		GregorianCalendar calendar = new GregorianCalendar(xmlCalendar
				.getYear(), xmlCalendar.getMonth() - 1, xmlCalendar.getDay(), //
				xmlCalendar.getHour(), xmlCalendar.getMinute(), xmlCalendar
						.getSecond());
		calendar.setTimeZone(xmlCalendar.getTimeZone(0));
		return calendar.getTime();
	}

	private void addRevocationData(ValidateResultType validateResult,
			RevocationData revocationData) {

		be.fedict.trust.xkms.extensions.ObjectFactory extensionsObjectFactory = new be.fedict.trust.xkms.extensions.ObjectFactory();
		RevocationDataMessageExtensionType revocationDataMessageExtension = extensionsObjectFactory
				.createRevocationDataMessageExtensionType();
		org.etsi.uri._01903.v1_3.ObjectFactory xadesObjectFactory = new org.etsi.uri._01903.v1_3.ObjectFactory();
		RevocationValuesType revocationValues = xadesObjectFactory
				.createRevocationValuesType();

		// Add OCSP responses
		OCSPValuesType ocspValues = xadesObjectFactory.createOCSPValuesType();
		for (OCSPRevocationData ocspRevocationData : revocationData
				.getOcspRevocationData()) {
			EncapsulatedPKIDataType encapsulatedPKIData = xadesObjectFactory
					.createEncapsulatedPKIDataType();
			encapsulatedPKIData.setValue(Base64.encode(ocspRevocationData
					.getData()));
			ocspValues.getEncapsulatedOCSPValue().add(encapsulatedPKIData);
		}
		revocationValues.setOCSPValues(ocspValues);

		// Add CRL's
		CRLValuesType crlValues = xadesObjectFactory.createCRLValuesType();
		for (CRLRevocationData crlRevocationData : revocationData
				.getCrlRevocationData()) {
			EncapsulatedPKIDataType encapsulatedPKIData = xadesObjectFactory
					.createEncapsulatedPKIDataType();
			encapsulatedPKIData.setValue(Base64.encode(crlRevocationData
					.getData()));
			crlValues.getEncapsulatedCRLValue().add(encapsulatedPKIData);
		}
		revocationValues.setCRLValues(crlValues);

		revocationDataMessageExtension.setRevocationValues(revocationValues);
		validateResult.getMessageExtension()
				.add(revocationDataMessageExtension);
	}
}
