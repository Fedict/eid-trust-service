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

import java.util.List;

import javax.ejb.Local;

import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;

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
}
