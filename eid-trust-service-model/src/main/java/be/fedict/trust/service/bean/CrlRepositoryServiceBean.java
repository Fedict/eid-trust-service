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

import javax.ejb.Singleton;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.crl.CachedCrlRepository;

/**
 * Singleton Crl Repository cache bean used if no persistent CRL cache is
 * available.
 * 
 * @author wvdhaute
 */
@Singleton
public class CrlRepositoryServiceBean {

	private Log LOG = LogFactory.getLog(CrlRepositoryServiceBean.class);

	private CachedCrlRepository cachedCrlRepository;

	/**
	 * Returns the {@link CachedCrlRepository} or <code>null</code> if not yet
	 * set.
	 */
	public CachedCrlRepository getCachedCrlRepository() {

		LOG.debug("get cached CRL repository");
		return this.cachedCrlRepository;
	}

	/**
	 * Sets the {@link CachedCrlRepository}.
	 * 
	 * @param cachedCrlRepository
	 *            the cached CRL repository
	 */
	public void setCachedCrlRepository(CachedCrlRepository cachedCrlRepository) {

		LOG.debug("set cached CRL repository");
		this.cachedCrlRepository = cachedCrlRepository;
	}

	/**
	 * Resets the {@link CachedCrlRepository}.
	 */
	public void resetCachedCrlRepository() {

		LOG.debug("reset cached CRL repository");
		this.cachedCrlRepository = null;
	}

}
