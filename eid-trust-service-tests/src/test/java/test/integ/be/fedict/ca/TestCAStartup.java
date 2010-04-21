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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.integ.be.fedict.ca.servlet.InterCACrlServlet;

public class TestCAStartup {

	private static final Log LOG = LogFactory.getLog(TestCAStartup.class);

	private static TestCA testCA;

	@BeforeClass
	public static void oneTimeSetUp() throws Exception {

		testCA = new TestCA();
		testCA.start();

		LOG.debug("Root CA       : " + testCA.getPath()
				+ TestCA.ROOT_CA_CONTEXT_PATH);
		LOG.debug("Root CA CRL   : " + testCA.getPath()
				+ TestCA.ROOT_CA_CRL_CONTEXT_PATH);

		LOG.debug("Inter CA      : " + testCA.getPath()
				+ TestCA.INTER_CA_CONTEXT_PATH);
		LOG.debug("Inter CA Key  : " + testCA.getPath()
				+ TestCA.INTER_CA_PRIVATE_KEY_CONTEXT_PATH);
		LOG.debug("Inter CA CRL  : " + testCA.getPath()
				+ TestCA.INTER_CA_CRL_CONTEXT_PATH);
		LOG.debug("Inter CA OCSP : " + testCA.getPath()
				+ TestCA.INTER_CA_OCSP_CONTEXT_PATH);
	}

	@AfterClass
	public static void oneTimeTearDown() throws Exception {

		testCA.stop();
	}

	@Test
	public void testCA() throws Exception {

		// fill up intermediate CA CRL revoked entries
		InterCACrlServlet.fillRevoked(500000);

		while (true) {
			Thread.sleep(1000);
		}

	}
}
