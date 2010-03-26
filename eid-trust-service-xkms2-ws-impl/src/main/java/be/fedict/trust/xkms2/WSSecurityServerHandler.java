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

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.SOAPConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;

import be.fedict.trust.service.KeyStoreUtils;
import be.fedict.trust.service.TrustService;
import be.fedict.trust.service.entity.WSSecurityConfigEntity;
import be.fedict.trust.service.exception.KeyStoreLoadException;

/**
 * WS-Security SOAP handler to optionally sign ( as configured in
 * {@link WSSecurityConfigEntity}.
 * 
 * @author wvdhaute
 * 
 */
public class WSSecurityServerHandler implements SOAPHandler<SOAPMessageContext> {

	private static final Log LOG = LogFactory
			.getLog(WSSecurityServerHandler.class);

	@PostConstruct
	public void postConstructCallback() {

		System
				.setProperty(
						"com.sun.xml.ws.fault.SOAPFaultBuilder.disableCaptureStackTrace",
						"true");
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<QName> getHeaders() {

		Set<QName> headers = new HashSet<QName>();
		headers
				.add(new QName(
						"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
						"Security"));
		return headers;
	}

	/**
	 * {@inheritDoc}
	 */
	public void close(MessageContext messageContext) {

		// empty
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean handleFault(SOAPMessageContext soapMessageContext) {

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean handleMessage(SOAPMessageContext soapMessageContext) {

		LOG.debug("handle message");

		Boolean outboundProperty = (Boolean) soapMessageContext
				.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		SOAPMessage soapMessage = soapMessageContext.getMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();

		if (true == outboundProperty.booleanValue()) {
			handleOutboundDocument(soapPart, soapMessageContext);
		}

		return true;
	}

	/**
	 * Handles the outbound SOAP message. Adds the WS Security Header containing
	 * a signed timestamp, and signed SOAP body.
	 * 
	 * @param document
	 */
	private void handleOutboundDocument(SOAPPart document,
			SOAPMessageContext soapMessageContext) {

		LOG.debug("handle outbound document");
		ServletContext servletContext = (ServletContext) soapMessageContext
				.get(MessageContext.SERVLET_CONTEXT);
		TrustService trustService = ServiceConsumerServletContextListener
				.getTrustService(servletContext);

		WSSecurityConfigEntity wsSecurityConfig = trustService
				.getWsSecurityConfig();

		if (wsSecurityConfig.isSigning()) {
			LOG.debug("adding WS-Security SOAP header");

			try {
				PrivateKeyEntry privateKeyEntry = KeyStoreUtils
						.loadPrivateKeyEntry(wsSecurityConfig);

				WSSecHeader wsSecHeader = new WSSecHeader();
				wsSecHeader.insertSecurityHeader(document);
				WSSecSignature wsSecSignature = new WSSecSignature();
				wsSecSignature
						.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);
				Crypto crypto = new ClientCrypto(
						(X509Certificate) privateKeyEntry.getCertificate(),
						privateKeyEntry.getPrivateKey());
				wsSecSignature.prepare(document, crypto, wsSecHeader);

				SOAPConstants soapConstants = org.apache.ws.security.util.WSSecurityUtil
						.getSOAPConstants(document.getDocumentElement());

				Vector<WSEncryptionPart> wsEncryptionParts = new Vector<WSEncryptionPart>();
				WSEncryptionPart wsEncryptionPart = new WSEncryptionPart(
						soapConstants.getBodyQName().getLocalPart(),
						soapConstants.getEnvelopeURI(), "Content");
				wsEncryptionParts.add(wsEncryptionPart);

				WSSecTimestamp wsSecTimeStamp = new WSSecTimestamp();
				wsSecTimeStamp.setTimeToLive(0);
				/*
				 * If ttl is zero then there will be no Expires element within
				 * the Timestamp. Eventually we want to let the service itself
				 * decide how long the message validity period is.
				 */
				wsSecTimeStamp.prepare(document);
				wsSecTimeStamp.prependToHeader(wsSecHeader);
				wsEncryptionParts.add(new WSEncryptionPart(wsSecTimeStamp
						.getId()));

				wsSecSignature.addReferencesToSign(wsEncryptionParts,
						wsSecHeader);

				wsSecSignature.prependToHeader(wsSecHeader);

				wsSecSignature.prependBSTElementToHeader(wsSecHeader);

				wsSecSignature.computeSignature();

			} catch (WSSecurityException e) {
				throw new RuntimeException("WSS4J error: " + e.getMessage(), e);
			} catch (KeyStoreLoadException e) {
				throw new RuntimeException("Failed to laod keystore: "
						+ e.getMessage(), e);
			}
		}

	}
}
