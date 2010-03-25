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

package be.fedict.trust.xkms2;

import javax.jws.HandlerChain;
import javax.jws.WebService;

import net.test.ping.PingPort;
import net.test.ping.Request;
import net.test.ping.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of XKMS2 Web Service JAX-WS Port.
 * 
 * @author fcorneli
 * 
 */
@WebService(endpointInterface = "net.test.ping.PingPort")
@HandlerChain(file = "ws-handlers.xml")
public class PingPortImpl implements PingPort {

	private static final Log LOG = LogFactory.getLog(PingPortImpl.class);

	public Response pingOperation(Request request) {

		LOG.debug("ping");
		Response response = new Response();
		return response;
	}

}
