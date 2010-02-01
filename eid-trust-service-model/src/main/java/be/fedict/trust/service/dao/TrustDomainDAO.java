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

package be.fedict.trust.service.dao;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.Local;

import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;

/**
 * Trust Domain DAO.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface TrustDomainDAO {

	/**
	 * Returns list of {@link TrustDomainEntity}'s
	 */
	List<TrustDomainEntity> listTrustDomains();

	/**
	 * Return {@link TrustDomainEntity} from specified name. Returns
	 * <code>null</code> if not found.
	 * 
	 * @param name
	 */
	TrustDomainEntity findTrustDomain(String name);

	/**
	 * Create a new {@link TrustDomainEntity}.
	 * 
	 * @param name
	 * @param crlRefreshCron
	 */
	TrustDomainEntity addTrustDomain(String name, String crlRefreshCron);

	/**
	 * Create a new {@link CertificateAuthorityEntity}.
	 * 
	 * @param crlUrl
	 * @param rootCaCertificate
	 * @param trustPoint
	 */
	CertificateAuthorityEntity addCertificateAuthority(String crlUrl,
			X509Certificate certificate, TrustPointEntity trustPoint);

	/**
	 * Return {@link TrustPointEntity} from specified name. Returns
	 * <code>null</code> if not found.
	 * 
	 * @param name
	 */
	TrustPointEntity findTrustPoint(String name);

	/**
	 * Create a new {@link TrustPointEntity}.
	 * 
	 * @param name
	 * @param crlRefreshCron
	 * @param trustDomain
	 * @param ca
	 */
	TrustPointEntity addTrustPoint(String name, String crlRefreshCron,
			TrustDomainEntity trustDomain, CertificateAuthorityEntity ca);

	/**
	 * Returns list of {@link TrustPointEntity}'s attached to the specified
	 * {@link TrustDomainEntity}.
	 * 
	 * @param trustDomain
	 */
	List<TrustPointEntity> listTrustPoints(TrustDomainEntity trustDomain);

	/**
	 * Returns list of {@link CertificateAuthorityEntity}'s for the specified
	 * {@link TrustPointEntity}.
	 * 
	 * @param trustPoint
	 */
	List<CertificateAuthorityEntity> listCertificateAuthorities(
			TrustPointEntity trustPoint);
}
