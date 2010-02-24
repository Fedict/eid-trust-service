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

package be.fedict.trust.client;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.List;

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3._2000._09.xmldsig_.KeyInfoType;
import org.w3._2000._09.xmldsig_.X509DataType;
import org.w3._2002._03.xkms_.KeyBindingType;
import org.w3._2002._03.xkms_.ObjectFactory;
import org.w3._2002._03.xkms_.QueryKeyBindingType;
import org.w3._2002._03.xkms_.StatusType;
import org.w3._2002._03.xkms_.ValidateRequestType;
import org.w3._2002._03.xkms_.ValidateResultType;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;
import org.w3._2002._03.xkms_wsdl.XKMSService;

import be.fedict.trust.xkms.extensions.TrustDomainMessageExtensionType;
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

	/**
	 * Main constructor
	 * 
	 * @param location
	 *            the location (host:port) of the XKMS2 web service
	 */
	public XKMS2Client(String location) {

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

	public boolean validate(List<X509Certificate> authnCertificateChain)
			throws CertificateEncodingException {

		return validate(null, authnCertificateChain);
	}

	public boolean validate(String trustDomain,
			List<X509Certificate> authnCertificateChain)
			throws CertificateEncodingException {
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
		 * Set optional trust domain message extension
		 */
		if (null != trustDomain) {
			be.fedict.trust.xkms.extensions.ObjectFactory extensionsObjectFactory = new be.fedict.trust.xkms.extensions.ObjectFactory();
			TrustDomainMessageExtensionType trustDomainMessageExtension = extensionsObjectFactory
					.createTrustDomainMessageExtensionType();
			trustDomainMessageExtension.setTrustDomain(trustDomain);
			validateRequest.getMessageExtension().add(
					trustDomainMessageExtension);
		}

		// TODO: WS trust via unilateral TLS trust model based on public key

		ValidateResultType validateResult = port.validate(validateRequest);

		if (null == validateResult) {
			throw new RuntimeException("missing ValidateResult element");
		}
		List<KeyBindingType> keyBindings = validateResult.getKeyBinding();
		for (KeyBindingType keyBinding : keyBindings) {
			// TODO better result verification
			StatusType status = keyBinding.getStatus();
			String statusValue = status.getStatusValue();
			LOG.debug("status: " + statusValue);
			if ("http://www.w3.org/2002/03/xkms#Valid".equals(statusValue)) {
				return true;
			}
		}
		return false;
	}
}
