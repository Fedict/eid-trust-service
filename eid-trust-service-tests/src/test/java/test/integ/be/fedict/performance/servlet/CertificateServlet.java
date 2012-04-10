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

package test.integ.be.fedict.performance.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import test.integ.be.fedict.performance.CAConfiguration;
import test.integ.be.fedict.performance.TestPKI;
import test.integ.be.fedict.trust.util.TestUtils;

public class CertificateServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(CertificateServlet.class);

	public static final String PATH = "certificate";

	public static final String CA_QUERY_PARAM = "ca";

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String caName = request.getParameter(CA_QUERY_PARAM);
		if (null == caName) {
			throw new ServletException("No CA name found.");
		}
		LOG.debug("get certificate for CA=" + caName);

		CAConfiguration ca = TestPKI.get().findCa(caName);
		if (null == ca) {
			throw new ServletException("CA Config not found for " + caName);
		}

		String pemCertificate = TestUtils.toPem(ca.getCertificate());

		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		out.print(pemCertificate);
		out.close();
	}

	public static String getPath(String caName) {
		return TestPKI.get().getPath() + "/" + PATH + "?" + CA_QUERY_PARAM
				+ "=" + caName;
	}
}
