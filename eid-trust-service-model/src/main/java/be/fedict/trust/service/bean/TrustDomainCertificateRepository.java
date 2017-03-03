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

import be.fedict.trust.CertificateRepository;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;

/**
 * Trust domain {@link CertificateRepository} that takes a
 * {@link TrustDomainEntity} as input and fills it up with its
 * {@link TrustPointEntity}'s certificates.
 */
public class TrustDomainCertificateRepository implements CertificateRepository {

	private final TrustDomainEntity trustDomain;

	public TrustDomainCertificateRepository(TrustDomainEntity trustDomain) {
		this.trustDomain = trustDomain;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isTrustPoint(X509Certificate certificate) {
		for (TrustPointEntity trustPoint : this.trustDomain.getTrustPoints()) {
			if (matches(trustPoint, certificate)) {
				return true;
			}
		}
		return false;
	}

	private boolean matches(TrustPointEntity trustPoint, X509Certificate certificate) {
		X509Certificate trustPointCertificate = trustPoint.getCertificateAuthority().getCertificate();

		return trustPointCertificate.getSubjectDN().equals(certificate.getSubjectDN()) &&
				trustPointCertificate.getPublicKey().equals(certificate.getPublicKey());
	}

}
