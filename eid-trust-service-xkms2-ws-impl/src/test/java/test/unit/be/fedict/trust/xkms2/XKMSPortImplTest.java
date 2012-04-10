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

package test.unit.be.fedict.trust.xkms2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.trust.client.jaxws.xkms.XKMSPortType;
import be.fedict.trust.client.jaxws.xkms.XKMSService;
import be.fedict.trust.xkms2.XKMSPortImpl;
import be.fedict.trust.xkms2.XKMSServiceFactory;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

public class XKMSPortImplTest {

	private static final Log LOG = LogFactory.getLog(XKMSPortImplTest.class);

	@Test
	public void test() throws Exception {
		int port = getFreePort();
		String address = "http://localhost:" + port + "/xkms2";
		HttpServer server = HttpServer.create(new InetSocketAddress(
				"localhost", port), 5);
		ExecutorService threads = Executors.newFixedThreadPool(5);
		server.setExecutor(threads);
		server.start();

		XKMSPortType xkmsPort = new XKMSPortImpl();
		Endpoint endpoint = Endpoint.create(xkmsPort);

		HttpContext httpContext = server.createContext("/xkms2");
		LOG.debug("http context attributes: " + httpContext.getAttributes());
		endpoint.publish(httpContext);

		assertTrue(endpoint.isPublished());

		HttpClient httpClient = new HttpClient();
		GetMethod getMethod = new GetMethod(address + "?wsdl");
		int statusCode = httpClient.executeMethod(getMethod);
		LOG.debug("status code: " + statusCode);
		assertEquals(HttpServletResponse.SC_OK, statusCode);
		LOG.debug("runtime WSDL: " + getMethod.getResponseBodyAsString());

		getMethod = new GetMethod(address + "?wsdl=1");
		statusCode = httpClient.executeMethod(getMethod);
		LOG.debug("status code: " + statusCode);
		assertEquals(HttpServletResponse.SC_OK, statusCode);
		LOG.debug("runtime WSDL: " + getMethod.getResponseBodyAsString());

		XKMSService xkmsService = XKMSServiceFactory.getInstance();
		XKMSPortType xkmsPortClient = xkmsService.getXKMSPort();
		BindingProvider bindingProviderClient = (BindingProvider) xkmsPortClient;
		bindingProviderClient.getRequestContext().put(
				BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);

		// EasyMock.replay(mockServletContext);
		// ValidateResultType validateResult = xkmsPortClient.validate(null);
		// EasyMock.verify(mockServletContext);
	}

	private static int getFreePort() throws Exception {
		ServerSocket serverSocket = new ServerSocket(0);
		int port = serverSocket.getLocalPort();
		serverSocket.close();
		return port;
	}
}
