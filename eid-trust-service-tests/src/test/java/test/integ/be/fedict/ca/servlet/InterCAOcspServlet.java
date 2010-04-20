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

package test.integ.be.fedict.ca.servlet;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.ocsp.RevokedInfo;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.BasicOCSPRespGenerator;
import org.bouncycastle.ocsp.CertificateStatus;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPRespGenerator;
import org.bouncycastle.ocsp.Req;
import org.bouncycastle.ocsp.RevokedStatus;

import test.integ.be.fedict.ca.TestCA;

public class InterCAOcspServlet extends HttpServlet {

	private static final Log LOG = LogFactory.getLog(InterCAOcspServlet.class);

	private static final long serialVersionUID = 1L;

	public static List<BigInteger> revokedSerialNumbers = new LinkedList<BigInteger>();

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		LOG.debug("InterCA OCSP");
		try {

			if (null == request.getContentType()
					|| !request.getContentType().equals(
							"application/ocsp-request")) {
				LOG.error("Wrong content type: " + request.getContentType());
				return;
			}

			int len = request.getContentLength();
			byte[] ocspRequestData = new byte[len];
			ServletInputStream reader = request.getInputStream();
			reader.read(ocspRequestData);

			OCSPReq ocspRequest = new OCSPReq(ocspRequestData);
			LOG.debug("OCSP request");

			BasicOCSPRespGenerator ocspRespGen = new BasicOCSPRespGenerator(
					TestCA.interCaKeyPair.getPublic());
			for (Req req : ocspRequest.getRequestList()) {
				if (revokedSerialNumbers.contains(req.getCertID()
						.getSerialNumber())) {
					LOG.debug("revoked: sn="
							+ req.getCertID().getSerialNumber().toString());
					ocspRespGen.addResponse(req.getCertID(), new RevokedStatus(
							new RevokedInfo(new DERGeneralizedTime(new Date()),
									null)));
				} else {
					ocspRespGen.addResponse(req.getCertID(),
							CertificateStatus.GOOD);
				}
			}

			BasicOCSPResp ocspResp = ocspRespGen.generate("SHA1WITHRSA",
					TestCA.interCaKeyPair.getPrivate(), null, new Date(), "BC");

			OCSPRespGenerator og = new OCSPRespGenerator();

			response.setContentType("application/ocsp-response");
			response.getOutputStream().write(
					(og.generate(OCSPRespGenerator.SUCCESSFUL, ocspResp))
							.getEncoded());

		} catch (Exception e) {
			LOG.error("Exception: " + e.getMessage(), e);
			throw new ServletException(e);
		}
	}

	public static void markRevoked(X509Certificate certificate, boolean revoked) {

		LOG.debug("mark revoked: certificate sn="
				+ certificate.getSerialNumber().toString() + " revoked="
				+ revoked);
		if (revoked) {
			revokedSerialNumbers.add(certificate.getSerialNumber());
		} else {
			revokedSerialNumbers.remove(certificate.getSerialNumber());
		}
	}
}
