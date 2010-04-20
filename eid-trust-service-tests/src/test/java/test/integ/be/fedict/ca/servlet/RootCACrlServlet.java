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
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import test.integ.be.fedict.ca.TestCA;
import test.integ.be.fedict.trust.util.TestUtils;

public class RootCACrlServlet extends HttpServlet {

	private static final Log LOG = LogFactory.getLog(RootCACrlServlet.class);

	private static final long serialVersionUID = 1L;

	public static List<BigInteger> revokedSerialNumbers = new LinkedList<BigInteger>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse response)
			throws ServletException, IOException {

		LOG.debug("Get RootCA CRL");
		try {
			DateTime now = new DateTime();
			DateTime thisUpdate = now.minusHours(3);
			DateTime nextUpdate = now.plusHours(3);
			X509CRL rootCaCrl = TestUtils.generateCrl2(TestCA.rootCaKeyPair
					.getPrivate(), TestCA.rootCa, thisUpdate, nextUpdate,
					revokedSerialNumbers);

			response.setContentType("text/plain");
			response.getOutputStream().write(rootCaCrl.getEncoded());
		} catch (Exception e) {
			LOG.error("Exception: " + e.getMessage(), e);
			throw new ServletException(e);
		}
	}

	public static void markRevoked(X509Certificate certificate, boolean revoked) {

		if (revoked) {
			revokedSerialNumbers.add(certificate.getSerialNumber());
		} else {
			revokedSerialNumbers.remove(certificate.getSerialNumber());
		}
	}
}
