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

package test.unit.be.fedict.perf.ocsp;

import static org.junit.Assert.*;

import java.security.Security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import be.fedict.perf.ocsp.CertificateRepository;

public class CertificateRepositoryTest {

	private static final Log LOG = LogFactory
			.getLog(CertificateRepositoryTest.class);

	@BeforeClass
	public static void beforeClass() throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testLoadCertificateRepository() throws Exception {
		// operate
		CertificateRepository certificateRepository = new CertificateRepository();

		// verify
		LOG.debug("number of certificates in repository: "
				+ certificateRepository.getSize());
		assertTrue(certificateRepository.getSize() > 1000);
	}
}
