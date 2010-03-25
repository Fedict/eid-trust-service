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

package test.integ.be.fedict.trust;

import java.security.Security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import be.fedict.trust.client.PingClient;

/**
 * eID Trust Service XKMS2 Integration Tests.
 * 
 * @author wvdhaute
 * 
 */
public class PingTest {

	private static final Log LOG = LogFactory.getLog(PingTest.class);

	private static final String location = "http://sebeco-dev-11:8080";

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testPing() throws Exception {
		LOG.debug("test ping");

		// setup
		PingClient client = new PingClient(location);

		// operate
		client.ping();
	}
}
