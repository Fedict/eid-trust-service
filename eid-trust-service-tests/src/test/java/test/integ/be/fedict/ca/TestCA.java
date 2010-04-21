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

package test.integ.be.fedict.ca;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.joda.time.DateTime;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.LocalConnector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletMapping;
import org.mortbay.jetty.servlet.SessionHandler;

import test.integ.be.fedict.ca.servlet.InterCACrlServlet;
import test.integ.be.fedict.ca.servlet.InterCAOcspServlet;
import test.integ.be.fedict.ca.servlet.InterCAPrivateKeyServlet;
import test.integ.be.fedict.ca.servlet.InterCAServlet;
import test.integ.be.fedict.ca.servlet.RootCACrlServlet;
import test.integ.be.fedict.ca.servlet.RootCAServlet;
import test.integ.be.fedict.trust.util.TestUtils;

public class TestCA {

	private static final Log LOG = LogFactory.getLog(TestCA.class);

	public static final String CA_HOST = "sebeco-dev-11";

	public static final String ROOT_CA_CONTEXT_PATH = "/rootca.crt";
	public static final String ROOT_CA_CRL_CONTEXT_PATH = "/rootca.crl";
	public static final String INTER_CA_CONTEXT_PATH = "/interca.crt";
	public static final String INTER_CA_PRIVATE_KEY_CONTEXT_PATH = "/interca.key";
	public static final String INTER_CA_CRL_CONTEXT_PATH = "/interca.crl";
	public static final String INTER_CA_OCSP_CONTEXT_PATH = "/interca/ocsp";

	private Server server;

	private String path;

	public static KeyPair rootCaKeyPair;
	public static X509Certificate rootCa;

	public static KeyPair interCaKeyPair;
	public static X509Certificate interCa;

	public void start() throws Exception {

		this.server = new Server();
		Connector connector = new LocalConnector();
		this.server.addConnector(connector);

		/*
		 * Add servlets
		 */
		addServlet(RootCAServlet.class, ROOT_CA_CONTEXT_PATH);
		addServlet(RootCACrlServlet.class, ROOT_CA_CRL_CONTEXT_PATH);
		addServlet(InterCAServlet.class, INTER_CA_CONTEXT_PATH);
		addServlet(InterCAPrivateKeyServlet.class,
				INTER_CA_PRIVATE_KEY_CONTEXT_PATH);
		addServlet(InterCACrlServlet.class, INTER_CA_CRL_CONTEXT_PATH);
		addServlet(InterCAOcspServlet.class, INTER_CA_OCSP_CONTEXT_PATH);

		server.start();
		createSocketConnector();

		/*
		 * Create self-signed Root CA, issuing CRL
		 */
		DateTime now = new DateTime();
		DateTime notBefore = now.minusYears(10);
		DateTime notAfter = now.plusYears(10);

		rootCaKeyPair = TestUtils.generateKeyPair();
		rootCa = TestUtils.generateCertificate(rootCaKeyPair.getPublic(),
				"CN=TestRoot", rootCaKeyPair.getPrivate(), null, notBefore,
				notAfter, "SHA512WithRSAEncryption", true, true, false, null,
				this.path + ROOT_CA_CRL_CONTEXT_PATH, new KeyUsage(
						KeyUsage.cRLSign));

		/*
		 * Create intermediate CA, ussing CRL and OCSP
		 */
		interCaKeyPair = TestUtils.generateKeyPair();
		interCa = TestUtils.generateCertificate(interCaKeyPair.getPublic(),
				"CN=TestInterCA", rootCaKeyPair.getPrivate(), rootCa,
				notBefore, notAfter, "SHA512WithRSAEncryption", true, true,
				false, null, this.path + ROOT_CA_CRL_CONTEXT_PATH,
				new KeyUsage(KeyUsage.cRLSign));

		LOG.debug("Test CA started...");
	}

	public void stop() throws Exception {

		this.server.stop();
		LOG.debug("Test CA stopped...");
	}

	private void addServlet(Class<? extends HttpServlet> servletClass,
			String contextPath) {

		Context context = new Context(null, new SessionHandler(),
				new SecurityHandler(), null, null);
		context.setContextPath(contextPath);
		// http://jira.codehaus.org/browse/JETTY-390
		context.setAllowNullPathInfo(true);
		this.server.addHandler(context);

		ServletHandler handler = context.getServletHandler();

		ServletHolder servletHolder = new ServletHolder();
		servletHolder.setClassName(servletClass.getName());
		servletHolder.setName(servletClass.getName());
		handler.addServlet(servletHolder);

		ServletMapping servletMapping = new ServletMapping();
		servletMapping.setServletName(servletClass.getName());
		servletMapping.setPathSpecs(new String[] { "/*", contextPath });
		handler.addServletMapping(servletMapping);
	}

	private void createSocketConnector() throws Exception {

		SocketConnector connector = new SocketConnector();
		connector.setHost(CA_HOST);
		this.server.addConnector(connector);
		if (this.server.isStarted()) {
			connector.start();
		} else {
			connector.open();
		}

		this.path = "http://" + CA_HOST + ":" + connector.getLocalPort();
	}

	public String getPath() {

		return this.path;
	}

}
