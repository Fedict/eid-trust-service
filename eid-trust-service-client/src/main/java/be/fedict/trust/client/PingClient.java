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

import java.net.ConnectException;
import java.text.MessageFormat;
import java.util.List;

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import net.test.ping.PingPort;
import net.test.ping.PingService;
import net.test.ping.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.xkms2.LoggingSoapHandler;
import be.fedict.trust.xkms2.PingServiceFactory;

import com.sun.xml.ws.client.ClientTransportException;

/**
 * Client component for the eID Trust Service XKMS2 web service.
 * 
 * @author fcorneli
 * 
 */
public class PingClient {

	private static final Log LOG = LogFactory.getLog(PingClient.class);

	private final PingPort port;

	/**
	 * Main constructor
	 * 
	 * @param location
	 *            the location (host:port) of the XKMS2 web service
	 */
	public PingClient(String location) {

		PingService pingService = PingServiceFactory.newInstance();
		port = pingService.getPingPort();
		String wsLocation = MessageFormat.format(
				"{0}/eid-trust-service-ws/ping", location);

		registerLoggerHandler(port);
		setEndpointAddress(wsLocation);
	}

	public void ping() throws ConnectException {

		Request request = new Request();
		try {
			port.pingOperation(request);
		} catch (ClientTransportException e) {
			throw new ConnectException(e.getMessage());
		} catch (Exception e) {
			LOG.error("exception: " + e.getMessage(), e);
		}
	}

	private void setEndpointAddress(String location) {

		LOG.debug("ws location=" + location);
		BindingProvider bindingProvider = (BindingProvider) port;
		bindingProvider.getRequestContext().put(
				BindingProvider.ENDPOINT_ADDRESS_PROPERTY, location);
	}

	/**
	 * Registers the logging SOAP handler on the given JAX-WS port component.
	 * 
	 * @param port
	 */
	protected void registerLoggerHandler(Object port) {

		BindingProvider bindingProvider = (BindingProvider) port;

		Binding binding = bindingProvider.getBinding();
		@SuppressWarnings("unchecked")
		List<Handler> handlerChain = binding.getHandlerChain();
		handlerChain.add(new LoggingSoapHandler());
		binding.setHandlerChain(handlerChain);
	}

}
