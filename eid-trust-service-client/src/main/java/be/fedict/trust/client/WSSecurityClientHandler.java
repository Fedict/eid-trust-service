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

import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

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
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.token.Timestamp;
import org.joda.time.DateTime;
import org.joda.time.Instant;

/**
 * WS-Security client SOAP handler that will validate the WS-Security header if
 * any. If the client provides a server certificate, the certificate in the
 * WS-Security header will be checked against this.
 * 
 * @author wvdhaute
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
			handleInboundDocument(soapPart, soapMessageContext);
		}

		return true;
	}

	/**
	 * Handles the inbound SOAP message. If a WS-Security header is present, it
	 * will be validates. If a server certificate was specified, it will be
	 * checked against the {@link X509Certificate} in the WS-Security header.
	 */
	private void handleInboundDocument(SOAPPart document,
			SOAPMessageContext soapMessageContext) {

		WSSecurityEngine securityEngine = WSSecurityEngine.getInstance();
		Crypto crypto = new ServerCrypto();

		Vector<WSSecurityEngineResult> wsSecurityEngineResults;
		try {
			@SuppressWarnings("unchecked")
			Vector<WSSecurityEngineResult> checkedWsSecurityEngineResults = securityEngine
					.processSecurityHeader(document, null, null, crypto);
			wsSecurityEngineResults = checkedWsSecurityEngineResults;
		} catch (WSSecurityException e) {
			LOG.debug("WS-Security error: " + e.getMessage(), e);
			throw createSOAPFaultException(ERROR_INVALID_SIGNATURE,
					"FailedCheck");
		}
		if (null == wsSecurityEngineResults) {
			LOG.debug("No WS-Security header to validate");
			return;
		}

		LOG.debug("WS-Security header validation");

		Timestamp timestamp = null;
		X509Certificate signingCertificate = null;
		Set<String> signedElements = null;
		for (WSSecurityEngineResult result : wsSecurityEngineResults) {
			@SuppressWarnings("unchecked")
			Set<String> resultSignedElements = (Set<String>) result
					.get(WSSecurityEngineResult.TAG_SIGNED_ELEMENT_IDS);
			if (null != resultSignedElements) {
				signedElements = resultSignedElements;
			}

			if (null != result.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE)) {
				signingCertificate = (X509Certificate) result
						.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
			}

			Timestamp resultTimestamp = (Timestamp) result
					.get(WSSecurityEngineResult.TAG_TIMESTAMP);
			if (null != resultTimestamp) {
				timestamp = resultTimestamp;
			}
		}

		if (null == signedElements) {
			throw createSOAPFaultException(ERROR_INVALID_SIGNATURE,
					"FailedCheck");
		}
		LOG.debug("signed elements: " + signedElements);

		/*
		 * Validate certificate
		 */
		if (null == signingCertificate) {
			throw createSOAPFaultException(ERROR_CERTIFICATE_MISSING,
					"InvalidSecurity");
		}
		if (null != serverCertificate
				&& !serverCertificate.equals(signingCertificate)) {
			throw createSOAPFaultException(ERROR_CERTIFICATE_MISMATCH,
					"FailedCheck");
		}

		/*
		 * Check timestamp.
		 */
		if (null == timestamp) {
			throw createSOAPFaultException(ERROR_TIMESTAMP_MISSING,
					"InvalidSecurity");
		}
		String timestampId = timestamp.getID();
		if (false == signedElements.contains(timestampId)) {
			throw createSOAPFaultException("Timestamp not signed",
					"FailedCheck");
		}
		Calendar created = timestamp.getCreated();
		DateTime createdDateTime = new DateTime(created);
		Instant createdInstant = createdDateTime.toInstant();
		Instant nowInstant = new DateTime().toInstant();
		long offset = Math.abs(createdInstant.getMillis()
				- nowInstant.getMillis());
		if (offset > maxTimestampOffset) {
			LOG.debug("timestamp offset: " + offset);
			LOG.debug("maximum allowed offset: " + maxTimestampOffset);
			throw createSOAPFaultException(ERROR_TIMESTAMP_OFFSET,
					"FailedCheck");
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
