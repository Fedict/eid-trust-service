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

package be.fedict.trust.client;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.token.Timestamp;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * WS-Security client SOAP handler that will validate the WS-Security header if
 * any. If the client provides a server certificate, the certificate in the
 * WS-Security header will be checked against this.
 * 
 * @author wvdhaute
 * @author Frank Cornelis
 */
public class WSSecurityClientHandler implements SOAPHandler<SOAPMessageContext> {

	private static final Log LOG = LogFactory
			.getLog(WSSecurityClientHandler.class);

	public static final String ERROR_INVALID_SIGNATURE = "The signature or decryption was invalid";
	public static final String ERROR_CERTIFICATE_MISMATCH = "The signing certificate does not match the specified server certificate";
	public static final String ERROR_CERTIFICATE_MISSING = "Missing Certificate in WS-Security header";
	public static final String ERROR_TIMESTAMP_MISSING = "Missing Timestamp in WS-Security header";
	public static final String ERROR_TIMESTAMP_OFFSET = "WS-Security Created Timestamp offset exceeded";

	public static final long defaultMaxTimestampOffset = 1000 * 60 * 5L;

	private X509Certificate serverCertificate;
	private long maxTimestampOffset;

	/**
	 * Main constructor.
	 */
	public WSSecurityClientHandler() {

		this.maxTimestampOffset = defaultMaxTimestampOffset;
	}

	/**
	 * Set the optional server {@link X509Certificate}. If specified and the
	 * trust service has message signing configured, the incoming
	 * {@link X509Certificate} will be checked against the specified server
	 * certificate.
	 * 
	 * @param serverCertificate
	 *            the server X509 certificate.
	 */
	public void setServerCertificate(X509Certificate serverCertificate) {

		this.serverCertificate = serverCertificate;
	}

	/**
	 * Set the maximum offset of the WS-Security timestamp ( in ms ). If not
	 * specified this will be defaulted to 5 minutes.
	 */
	public void setMaxWSSecurityTimestampOffset(
			long maxWSSecurityTimestampOffset) {

		this.maxTimestampOffset = maxWSSecurityTimestampOffset;
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

		Boolean outboundProperty = (Boolean) soapMessageContext
				.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		SOAPMessage soapMessage = soapMessageContext.getMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();

		if (false == outboundProperty.booleanValue()) {
			/*
			 * Validate incoming WS-Security header if present
			 */
			return handleInboundDocument(soapPart, soapMessageContext);
		}

		return true;
	}

	/**
	 * Handles the inbound SOAP message. If a WS-Security header is present, it
	 * will be validated. If a server certificate was specified, it will be
	 * checked against the {@link X509Certificate} in the WS-Security header.
	 */
	private boolean handleInboundDocument(SOAPPart document,
			SOAPMessageContext soapMessageContext) {
		WSSecurityEngine securityEngine = new WSSecurityEngine();
		WSSConfig wssConfig = WSSConfig.getNewInstance();
		securityEngine.setWssConfig(wssConfig);

		List<WSSecurityEngineResult> wsSecurityEngineResults;
		try {
			Crypto crypto = new ServerCrypto();
			wsSecurityEngineResults = securityEngine.processSecurityHeader(
					document, null, null, crypto);
		} catch (WSSecurityException e) {
			LOG.debug("WS-Security error: " + e.getMessage(), e);
			throw new SecurityException("WS-Security error: " + e.getMessage(),
					e);
		}
		if (null == wsSecurityEngineResults) {
			LOG.debug("No WS-Security header to validate");
			return true;
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
			throw new SecurityException(ERROR_TIMESTAMP_MISSING);
		}

		// WS-Security signature
		WSSecurityEngineResult signActionResult = WSSecurityUtil
				.fetchActionResult(wsSecurityEngineResults, WSConstants.SIGN);
		if (null == signActionResult) {
			throw new SecurityException("missing WS-Security signature");
		}
		X509Certificate signingCertificate = (X509Certificate) signActionResult
				.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);

		/*
		 * Validate certificate
		 */
		if (null == signingCertificate) {
			throw new SecurityException(ERROR_CERTIFICATE_MISSING);
		}
		if (null != this.serverCertificate
				&& !serverCertificate.equals(signingCertificate)) {
			throw new SecurityException(ERROR_CERTIFICATE_MISMATCH);
		}

		return true;
	}
}
