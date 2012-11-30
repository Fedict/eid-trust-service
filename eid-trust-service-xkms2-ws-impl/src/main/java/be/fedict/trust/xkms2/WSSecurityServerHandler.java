/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2012 FedICT.
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
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.xml.crypto.dsig.Reference;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.SOAPConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;
import org.apache.ws.security.message.token.Timestamp;
import org.apache.ws.security.util.WSSecurityUtil;
import org.joda.time.DateTime;
import org.joda.time.Instant;

import be.fedict.trust.service.KeyStoreUtils;
import be.fedict.trust.service.TrustService;
import be.fedict.trust.service.entity.WSSecurityConfigEntity;
import be.fedict.trust.service.exception.KeyStoreLoadException;

/**
 * WS-Security SOAP handler to optionally sign ( as configured in
 * {@link WSSecurityConfigEntity}.
 * 
 * @author wvdhaute
 * @author Frank Cornelis
 * 
 */
public class WSSecurityServerHandler implements SOAPHandler<SOAPMessageContext> {

	private static final Log LOG = LogFactory
			.getLog(WSSecurityServerHandler.class);

	private Long maxWsSecurityTimestampOffset = 1000 * 60 * 5L;

	@PostConstruct
	public void postConstructCallback() {

		System.setProperty(
				"com.sun.xml.ws.fault.SOAPFaultBuilder.disableCaptureStackTrace",
				"true");
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<QName> getHeaders() {

		Set<QName> headers = new HashSet<QName>();
		headers.add(new QName(
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
		} else {
			handleInboundDocument(soapPart, soapMessageContext);
		}

		return true;
	}

	/**
	 * Handles the inbound SOAP message. If a WS-Security header is present,
	 * will validate body and timestamp being signed. No validation of the
	 * embedded certificate will be done.
	 */
	@SuppressWarnings("unchecked")
	private void handleInboundDocument(SOAPPart document,
			SOAPMessageContext soapMessageContext) {

		LOG.debug("handle inbound document");

		WSSecurityEngine securityEngine = new WSSecurityEngine();
		WSSConfig wssConfig = WSSConfig.getNewInstance();
		securityEngine.setWssConfig(wssConfig);

		List<WSSecurityEngineResult> wsSecurityEngineResults;
		try {
			wsSecurityEngineResults = securityEngine.processSecurityHeader(
					document, null, null, null);
		} catch (WSSecurityException e) {
			LOG.debug("WS-Security error: " + e.getMessage(), e);
			throw createSOAPFaultException(
					"The signature or decryption was invalid", "FailedCheck");
		}
		LOG.debug("results: " + wsSecurityEngineResults);
		if (null == wsSecurityEngineResults) {
			LOG.debug("No WS-Security header present");
			return;
		}

		LOG.debug("WS-Security header validation");
		// WS-Security timestamp validation
		WSSecurityEngineResult timeStampActionResult = WSSecurityUtil
				.fetchActionResult(wsSecurityEngineResults, WSConstants.TS);
		if (null == timeStampActionResult) {
			throw new SecurityException("no WS-Security timestamp result");
		}
		Timestamp receivedTimestamp = (Timestamp) timeStampActionResult
				.get(WSSecurityEngineResult.TAG_TIMESTAMP);
		if (null == receivedTimestamp) {
			throw new SecurityException("missing WS-Security timestamp");
		}

		Date created = receivedTimestamp.getCreated();
		DateTime createdDateTime = new DateTime(created);
		Instant createdInstant = createdDateTime.toInstant();
		Instant nowInstant = new DateTime().toInstant();
		long offset = Math.abs(createdInstant.getMillis()
				- nowInstant.getMillis());
		if (offset > maxWsSecurityTimestampOffset) {
			LOG.debug("timestamp offset: " + offset);
			LOG.debug("maximum allowed offset: " + maxWsSecurityTimestampOffset);
			throw createSOAPFaultException(
					"WS-Security Created Timestamp offset exceeded",
					"FailedCheck");
		}
	}

	/**
	 * Handles the outbound SOAP message. Adds the WS Security Header containing
	 * a signed timestamp, and signed SOAP body.
	 */
	private void handleOutboundDocument(SOAPPart soapPart,
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
				X509Certificate certificate = (X509Certificate) privateKeyEntry
						.getCertificate();
				PrivateKey privateKey = privateKeyEntry.getPrivateKey();

				WSSecHeader wsSecHeader = new WSSecHeader();
				wsSecHeader.insertSecurityHeader(soapPart);

				WSSecTimestamp wsSecTimeStamp = new WSSecTimestamp();
				wsSecTimeStamp.setTimeToLive(0);
				wsSecTimeStamp.build(soapPart, wsSecHeader);

				ClientCrypto crypto = new ClientCrypto(certificate, privateKey);
				WSSConfig wssConfig = new WSSConfig();
				wssConfig.setWsiBSPCompliant(false);
				WSSecSignature sign = new WSSecSignature(wssConfig);
				sign.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);
				sign.prepare(soapPart, crypto, wsSecHeader);
				sign.appendBSTElementToHeader(wsSecHeader);
				Vector<WSEncryptionPart> signParts = new Vector<WSEncryptionPart>();
				signParts.add(new WSEncryptionPart(wsSecTimeStamp.getId()));
				SOAPConstants soapConstants = WSSecurityUtil
						.getSOAPConstants(soapPart.getDocumentElement());
				signParts.add(new WSEncryptionPart(soapConstants.getBodyQName()
						.getLocalPart(), soapConstants.getEnvelopeURI(),
						"Content"));
				sign.addReferencesToSign(signParts, wsSecHeader);
				List<Reference> referenceList = sign.addReferencesToSign(
						signParts, wsSecHeader);
				sign.computeSignature(referenceList, false, null);

			} catch (WSSecurityException e) {
				trustService.logAudit("WS-Security error: " + e.getMessage());
				throw new RuntimeException("WSS4J error: " + e.getMessage(), e);
			} catch (KeyStoreLoadException e) {
				trustService.logAudit("Load keystore error: " + e.getMessage());
				throw new RuntimeException("Failed to laod keystore: "
						+ e.getMessage(), e);
			}
		}
	}

	public static SOAPFaultException createSOAPFaultException(
			String faultString, String wsseFaultCode) {

		SOAPFault soapFault;
		try {
			SOAPFactory soapFactory = SOAPFactory.newInstance();
			soapFault = soapFactory
					.createFault(
							faultString,
							new QName(
									"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
									wsseFaultCode, "wsse"));
		} catch (SOAPException e) {
			throw new RuntimeException("SOAP error");
		}
		return new SOAPFaultException(soapFault);
	}

}
