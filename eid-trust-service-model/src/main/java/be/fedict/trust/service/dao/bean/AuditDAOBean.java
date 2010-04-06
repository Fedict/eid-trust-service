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

package be.fedict.trust.service.dao.bean;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.dao.AuditDAO;
import be.fedict.trust.service.entity.AuditEntity;

/**
 * Audit DAO Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
public class AuditDAOBean implements AuditDAO {

	private static final Log LOG = LogFactory.getLog(AuditDAOBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<AuditEntity> listAudits() {

		LOG.debug("list audits");
		Query query = this.entityManager
				.createNamedQuery(AuditEntity.QUERY_LIST_ALL);
		return (List<AuditEntity>) query.getResultList();
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearAudits() {

		LOG.debug("clear audits");
		Query query = this.entityManager
				.createNamedQuery(AuditEntity.REMOVE_ALL);
		int removed = query.executeUpdate();
		LOG.debug("# removed: " + removed);
	}

	/**
	 * {@inheritDoc}
	 */
	public AuditEntity logAudit(String message) {

		AuditEntity audit = new AuditEntity(new Date(), message);
		this.entityManager.persist(audit);
		LOG.error("audit: date=" + audit.getDate().toString() + " message="
				+ audit.getMessage());
		return audit;
	}
}
