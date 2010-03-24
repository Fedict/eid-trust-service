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

package be.fedict.trust.service.bean;

import java.security.cert.X509Certificate;
import java.util.Date;

import be.fedict.trust.RevocationData;
import be.fedict.trust.TrustLinkerResult;
import be.fedict.trust.ocsp.OcspRepository;
import be.fedict.trust.ocsp.OcspTrustLinker;
import be.fedict.trust.service.SnmpConstants;
import be.fedict.trust.service.snmp.SNMPInterceptor;

/**
 * Wrapper class for the jtrust {@link OcspTrustLinker}. This to be able to log
 * the number of failed OCSP requests.
 * 
 * @author wvdhaute
 * 
 */
public class TrustServiceOcspTrustLinker extends OcspTrustLinker {

	public TrustServiceOcspTrustLinker(OcspRepository ocspRepository) {

		super(ocspRepository);
	}

	@Override
	public TrustLinkerResult hasTrustLink(X509Certificate childCertificate,
			X509Certificate certificate, Date validationDate,
			RevocationData revocationData) {

		TrustLinkerResult result = super.hasTrustLink(childCertificate,
				certificate, validationDate, revocationData);
		if (null == result) {
			SNMPInterceptor.increment(SnmpConstants.OCSP_FAILURES,
					SnmpConstants.SNMP_SERVICE, 1L);
		}

		return result;
	}
}
