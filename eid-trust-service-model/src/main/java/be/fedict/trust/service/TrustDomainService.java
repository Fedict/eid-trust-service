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

package be.fedict.trust.service;

import java.security.cert.CertificateException;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Timer;

import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.exception.TrustPointAlreadyExistsException;

/**
 * Trust domain service.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface TrustDomainService {

	/**
	 * List all {@link TrustDomainEntity}'s.
	 */
	List<TrustDomainEntity> listTrustDomains();

	/**
	 * List all {@link TrustPointEntity}'s for the specified
	 * {@link TrustDomainEntity}.
	 * 
	 * @param trustDomain
	 */
	List<TrustPointEntity> listTrustPoints(TrustDomainEntity trustDomain);

	/**
	 * Save the changes to the specified {@link TrustDomainEntity}
	 * 
	 * @param trustDomain
	 * 
	 * @throws InvalidCronExpressionException
	 */
	void save(TrustDomainEntity trustDomain)
			throws InvalidCronExpressionException;

	/**
	 * Save the changes to the specified {@link TrustPointEntity}
	 * 
	 * @param trustPoint
	 * 
	 * @throws InvalidCronExpressionException
	 */
	void save(TrustPointEntity trustPoint)
			throws InvalidCronExpressionException;

	/**
	 * Add a new {@link TrustPointEntity}.
	 * 
	 * @param crlRefreshCron
	 * @param trustDomain
	 * @param certificateBytes
	 * 
	 * @throws TrustPointAlreadyExistsException
	 * @throws CertificateException
	 * @throws InvalidCronExpressionException
	 */
	TrustPointEntity addTrustPoint(String crlRefreshCron,
			TrustDomainEntity trustDomain, byte[] certificateBytes)
			throws TrustPointAlreadyExistsException, CertificateException,
			InvalidCronExpressionException;

	/**
	 * Sets the specified {@link TrustDomainEntity} as default.
	 * 
	 * @param trustDomain
	 */
	void setDefault(TrustDomainEntity trustDomain);

	/**
	 * Removes the selected {@link TrustPointEntity} and all related
	 * {@link CertificateAuthorityEntity}'s. Removes existing {@link Timer}'s.
	 * 
	 * @param trustPoint
	 */
	void remove(TrustPointEntity trustPoint);
}
