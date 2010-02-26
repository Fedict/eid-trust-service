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
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.jws.WebService;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3._2000._09.xmldsig_.KeyInfoType;
import org.w3._2000._09.xmldsig_.X509DataType;
import org.w3._2002._03.xkms_.KeyBindingType;
import org.w3._2002._03.xkms_.MessageExtensionAbstractType;
import org.w3._2002._03.xkms_.ObjectFactory;
import org.w3._2002._03.xkms_.QueryKeyBindingType;
import org.w3._2002._03.xkms_.StatusType;
import org.w3._2002._03.xkms_.ValidateRequestType;
import org.w3._2002._03.xkms_.ValidateResultType;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

import be.fedict.trust.service.TrustService;
import be.fedict.trust.service.exception.TrustDomainNotFoundException;
import be.fedict.trust.xkms.extensions.TrustDomainMessageExtensionType;

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
		for (MessageExtensionAbstractType messageExtension : body
				.getMessageExtension()) {
			if (messageExtension instanceof TrustDomainMessageExtensionType) {
				TrustDomainMessageExtensionType trustDomainExtension = (TrustDomainMessageExtensionType) messageExtension;
				trustDomain = trustDomainExtension.getTrustDomain();
			} else {
				LOG.error("Invalid message extension element: "
						+ messageExtension.getClass());
				return createResultResponse(ResultMajorCode.SENDER,
						ResultMinorCode.OPTIONAL_ELEMENT_NOT_SUPPORTED);
			}
		}

		boolean validationResult;
		try {
			validationResult = this.trustService.isValid(trustDomain,
					certificateChain);
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
		if (true == validationResult) {
			statusValue = "http://www.w3.org/2002/03/xkms#Valid";
		} else {
			statusValue = "http://www.w3.org/2002/03/xkms#Invalid";
		}
		status.setStatusValue(statusValue);

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
}
