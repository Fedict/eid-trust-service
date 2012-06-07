/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2010 FedICT.
 * Copyright (C) 2005-2007 Frank Cornelis.
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

package be.fedict.trust.service.ocsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.BasicOCSPRespGenerator;
import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.CertificateStatus;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.ocsp.OCSPRespGenerator;
import org.bouncycastle.ocsp.Req;
import org.bouncycastle.ocsp.RevokedStatus;

import be.fedict.trust.service.ValidationService;

public class OCSPResponderServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(OCSPResponderServlet.class);

	public static final String OCSP_REQUEST_CONTENT_TYPE = "application/ocsp-request";

	@EJB
	private ValidationService validationService;

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter printWriter = response.getWriter();
		printWriter.println("<html>");
		{
			printWriter.println("<head>");
			printWriter.println("<title>OCSP Responder</title>");
			printWriter.println("</head>");
		}
		{
			printWriter.println("<body>");
			{
				printWriter.println("<h1>OCSP Responder</h1>");
				printWriter.println("<p>");
				printWriter
						.println("Please use the protocol as described in RFC2560.");
				printWriter.println("</p>");
			}
			printWriter.println("</body>");
		}
		printWriter.println("</html>");
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String contentType = request.getContentType();
		if (false == OCSP_REQUEST_CONTENT_TYPE.equals(contentType)) {
			LOG.error("incorrect content type: " + contentType);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		InputStream ocspRequestInputStream = request.getInputStream();
		OCSPReq ocspReq = new OCSPReq(ocspRequestInputStream);

		Req[] requestList = ocspReq.getRequestList();
		if (1 != requestList.length) {
			LOG.error("OCSP request list size not 1: " + requestList.length);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		Req ocspRequest = requestList[0];

		CertificateID certificateID = ocspRequest.getCertID();
		LOG.debug("certificate Id hash algo OID: "
				+ certificateID.getHashAlgOID());
		if (false == CertificateID.HASH_SHA1.equals(certificateID
				.getHashAlgOID())) {
			LOG.debug("only supporting SHA1 hash algo");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		BigInteger serialNumber = certificateID.getSerialNumber();
		byte[] issuerNameHash = certificateID.getIssuerNameHash();
		byte[] issuerKeyHash = certificateID.getIssuerKeyHash();
		LOG.debug("serial number: " + serialNumber);
		LOG.debug("issuer name hash: "
				+ new String(Hex.encodeHex(issuerNameHash)));
		LOG.debug("issuer key hash: "
				+ new String(Hex.encodeHex(issuerKeyHash)));

		boolean valid = this.validationService.validate(serialNumber,
				issuerNameHash, issuerKeyHash);

		PrivateKeyEntry privateKeyEntry = this.validationService
				.getPrivateKeyEntry();
		if (null == privateKeyEntry) {
			LOG.debug("missing service identity");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		X509Certificate certificate = (X509Certificate) privateKeyEntry
				.getCertificate();
		PublicKey publicKey = certificate.getPublicKey();
		PrivateKey privateKey = privateKeyEntry.getPrivateKey();
		try {
			BasicOCSPRespGenerator basicOCSPRespGenerator = new BasicOCSPRespGenerator(
					publicKey);
			CertificateStatus certificateStatus;
			if (valid) {
				certificateStatus = CertificateStatus.GOOD;
			} else {
				certificateStatus = new RevokedStatus(new Date(),
						CRLReason.unspecified);
			}
			basicOCSPRespGenerator
					.addResponse(certificateID, certificateStatus);
			BasicOCSPResp basicOCSPResp = basicOCSPRespGenerator.generate(
					"SHA1WITHRSA", privateKey, null, new Date(),
					BouncyCastleProvider.PROVIDER_NAME);
			OCSPRespGenerator ocspRespGenerator = new OCSPRespGenerator();
			OCSPResp ocspResp = ocspRespGenerator.generate(
					OCSPRespGenerator.SUCCESSFUL, basicOCSPResp);
			response.setContentType("application/ocsp-response");
			response.getOutputStream().write(ocspResp.getEncoded());
		} catch (Exception e) {
			LOG.error("OCSP generator error: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}
}
