/*
 * SafeOnline project.
 *
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package be.fedict.trust.xkms2;

import java.net.URL;

import javax.xml.namespace.QName;

import net.test.ping.PingService;

public class PingServiceFactory {

	private PingServiceFactory() {

		// empty
	}

	/**
	 * Gives back a new instance of a ping service JAX-WS stub.
	 * 
	 */
	public static PingService newInstance() {

		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		URL wsdlUrl = classLoader.getResource("ping.wsdl");
		if (null == wsdlUrl)
			throw new RuntimeException("ping WSDL not found");
		PingService service = new PingService(wsdlUrl, new QName(
				"urn:net:test:ping", "PingService"));
		return service;
	}
}
