/*
 * eID Trust Service Project.
 * Copyright (C) 2009 FedICT.
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

import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Client component for the eID Trust Service XKMS2 web service.
 * 
 * @author fcorneli
 * 
 */
public class XKMS2Client {

	private static final Log LOG = LogFactory.getLog(XKMS2Client.class);

	public boolean validate(List<X509Certificate> authnCertificateChain) {
		LOG.debug("validate: "
				+ authnCertificateChain.get(0).getSubjectX500Principal());
		return false;
	}
}
