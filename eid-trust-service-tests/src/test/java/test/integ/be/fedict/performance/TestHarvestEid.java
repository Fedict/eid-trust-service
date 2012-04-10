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

package test.integ.be.fedict.performance;

import java.net.URI;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.trust.crl.OnlineCrlRepository;

/**
 * Unit test that harvests all BeId published CRLs and dumps the # entries they
 * contain Used to simulate the BeId PKI with an exact copy of CA's and size of
 * CRLs
 */
public class TestHarvestEid {

	private static final Log LOG = LogFactory.getLog(TestHarvestEid.class);

	private static final String BEID_URI = "http://crl.eid.belgium.be";

	private static final String URL_START = "<a href=\"";
	private static final String URL_STOP = "</a>";

	@Test
	public void testHarvestEid() throws Exception {

		// Operate: fetch CRL urls
		HttpClient httpClient = new HttpClient();
		GetMethod getMethod = new GetMethod(BEID_URI);
		httpClient.executeMethod(getMethod);

		String content = getMethod.getResponseBodyAsString();
		List<String> crlPaths = new LinkedList<String>();
		int start = content.indexOf(URL_START);
		int end = content.indexOf(URL_STOP);
		while (-1 != start) {
			String ahref = content.substring(start + URL_START.length(), end);
			String path = ahref.substring(0, ahref.indexOf("\">"));
			if (path.contains(".crl")) {
				crlPaths.add(path);
			}
			content = content.substring(end + URL_STOP.length());
			start = content.indexOf(URL_START);
			end = content.indexOf(URL_STOP);
		}

		// Setup
		OnlineCrlRepository onlineCrlRepository = new OnlineCrlRepository();

		// Operate: harvest
		List<CrlInfo> crlInfos = new LinkedList<CrlInfo>();
		for (String path : crlPaths) {
			URI crlURI = new URI(BEID_URI + "/" + path);
			X509CRL crl = onlineCrlRepository.findCrl(crlURI, null, null);
			int entries = 0;
			Set<? extends X509CRLEntry> crlEntries = crl
					.getRevokedCertificates();
			if (null != crlEntries) {
				entries = crlEntries.size();
			}
			crlInfos.add(new CrlInfo(crlURI.toString(), crl.getIssuerDN()
					.toString(), entries));
		}

		// Verify: output
		Random random = new Random();
		for (CrlInfo crlInfo : crlInfos) {
			LOG.debug(crlInfo.getUrl() + " : " + "TestPKI.get().addSaveCa(\""
					+ crlInfo.getIssuer() + "\", \"CN=root" + random.nextInt(2)
					+ "\", " + crlInfo.getEntries() + ", 0);");
		}
	}

	class CrlInfo {

		private final String url;
		private final String issuer;
		private final int entries;

		public CrlInfo(String url, String issuer, int entries) {
			this.url = url;
			this.issuer = issuer;
			this.entries = entries;
		}

		public String getUrl() {
			return url;
		}

		public String getIssuer() {
			return issuer;
		}

		public int getEntries() {
			return entries;
		}
	}
}
