/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2012 FedICT.
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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.trust.service.dao.AuditDAO;
import be.fedict.trust.service.entity.AuditEntity;

/**
 * Audit DAO Bean implementation.
 * 
 * @author wvdhaute
 * @author Frank Cornelis
 * 
 */
@Stateless
@LocalBinding(jndiBinding = AuditDAO.JNDI_BINDING)
public class AuditDAOBean implements AuditDAO {

	private static final Log LOG = LogFactory.getLog(AuditDAOBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	@SuppressWarnings("unchecked")
	public List<AuditEntity> listAudits() {
		Query query = this.entityManager
				.createNamedQuery(AuditEntity.QUERY_LIST_ALL);
		return (List<AuditEntity>) query.getResultList();
	}

	public void clearAudits() {
		Query query = this.entityManager
				.createNamedQuery(AuditEntity.REMOVE_ALL);
		int removed = query.executeUpdate();
		LOG.debug("# removed audit records: " + removed);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public AuditEntity logAudit(String message) {
		AuditEntity audit = new AuditEntity(message);
		this.entityManager.persist(audit);
		LOG.debug("audit: date=" + audit.getAuditDate().toString()
				+ " message=" + audit.getMessage());
		return audit;
	}
}
