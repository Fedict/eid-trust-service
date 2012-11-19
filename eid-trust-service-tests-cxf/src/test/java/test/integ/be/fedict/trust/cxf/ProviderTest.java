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

package test.integ.be.fedict.trust.cxf;

import static org.junit.Assert.assertEquals;

import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;

import javax.xml.ws.spi.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.commons.eid.jca.BeIDProvider;
import be.fedict.trust.client.XKMS2Client;

public class ProviderTest {

	private static final Log LOG = LogFactory.getLog(ProviderTest.class);

	@Test
	public void testProvider() {
		Provider provider = Provider.provider();
		LOG.debug("provider: " + provider.getClass().getName());
		assertEquals("org.apache.cxf.jaxws22.spi.ProviderImpl", provider
				.getClass().getName());
	}

	@Test
	public void testXKMS2Client() throws Exception {
		LOG.debug("loading eID certificate chain...");
		Security.addProvider(new BeIDProvider());
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		Certificate[] certificateChain = keyStore
				.getCertificateChain("Authentication");

		LOG.debug("creating XKMS client...");
		//String xkms2Url = "https://www.e-contract.be/eid-trust-service-ws/xkms2";
		String xkms2Url = "http://localhost/eid-trust-service-ws/xkms2";
		XKMS2Client xkms2Client = new XKMS2Client(xkms2Url);
		//xkms2Client.setProxy("proxy.yourict.net", 8080);

		LOG.debug("invoking XKMS client...");
		xkms2Client.validate(certificateChain);
		LOG.debug("done");
	}
}
