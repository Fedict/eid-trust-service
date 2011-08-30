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

import be.fedict.trust.BelgianTrustValidatorFactory;
import be.fedict.trust.NetworkConfig;
import be.fedict.trust.client.HAXKMS2Client;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;
import test.integ.be.fedict.trust.util.TestUtils;

import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * eID Trust Service HA XKMS2 client test.
 * 
 * @author wvdhaute
 */
public class HAXKMSTest {

	private static final Log LOG = LogFactory.getLog(HAXKMSTest.class);

	// private static final NetworkConfig NETWORK_CONFIG = new NetworkConfig(
	// "proxy.yourict.net", 8080);
	private static final NetworkConfig NETWORK_CONFIG = null;

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testValidateEIDCertificate() throws Exception {
		LOG.debug("validate eID authentication certificate.");

		List<X509Certificate> authnCertificateChain = TestUtils
				.getAuthnCertificateChain();

		HAXKMS2Client client = new HAXKMS2Client(TestUtils.XKMS_WS_LOCATION
				+ "/foo",
				BelgianTrustValidatorFactory
						.createTrustValidator(NETWORK_CONFIG));
		client.validate(authnCertificateChain);
	}
}
