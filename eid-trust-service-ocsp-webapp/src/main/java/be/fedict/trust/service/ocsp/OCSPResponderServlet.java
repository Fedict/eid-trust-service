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
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class OCSPResponderServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

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

	}
}
