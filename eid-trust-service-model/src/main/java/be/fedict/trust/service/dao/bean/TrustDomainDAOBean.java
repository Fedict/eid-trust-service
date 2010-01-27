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

package be.fedict.trust.service.dao.bean;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.TrustDomainEntity;

/**
 * Trust Domain DAO Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
@LocalBinding(jndiBinding = TrustDomainDAO.JNDI_BINDING)
public class TrustDomainDAOBean implements TrustDomainDAO {

	private static final Log LOG = LogFactory.getLog(TrustDomainDAOBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<TrustDomainEntity> listTrustDomains() {

		LOG.debug("list trust domains");
		Query query = this.entityManager.createQuery("FROM TrustDomainEntity");
		return (List<TrustDomainEntity>) query.getResultList();
	}

	/**
	 * {@inheritDoc}
	 */
	public TrustDomainEntity findTrustDomain(String name) {

		LOG.debug("find trust domain: " + name);
		return this.entityManager.find(TrustDomainEntity.class, name);
	}
}
