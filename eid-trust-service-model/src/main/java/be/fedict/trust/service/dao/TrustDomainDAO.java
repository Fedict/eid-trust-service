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

import java.util.List;

import javax.ejb.Local;

import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.entity.TrustDomainEntity;

/**
 * Trust Domain DAO.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface TrustDomainDAO {

	public static final String JNDI_BINDING = TrustServiceConstants.JNDI_CONTEXT
			+ "/TrustDomainDAOBean";

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
}
