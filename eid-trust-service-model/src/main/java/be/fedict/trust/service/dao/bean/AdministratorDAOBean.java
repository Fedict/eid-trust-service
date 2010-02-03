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

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.dao.AdministratorDAO;
import be.fedict.trust.service.entity.AdminEntity;

/**
 * Administrator DAO Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
public class AdministratorDAOBean implements AdministratorDAO {

	private static final Log LOG = LogFactory
			.getLog(AdministratorDAOBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<AdminEntity> listAdmins() {

		LOG.debug("list admins");
		Query query = this.entityManager
				.createNamedQuery(AdminEntity.QUERY_LIST_ALL);
		return (List<AdminEntity>) query.getResultList();
	}

	/**
	 * {@inheritDoc}
	 */
	public AdminEntity findAdmin(PublicKey publicKey) {

		LOG.debug("find admin");
		for (AdminEntity admin : listAdmins()) {
			if (admin.getPublicKey().equals(publicKey)) {
				LOG.debug("found admin: " + admin.getId());
				return admin;
			}
		}
		LOG.debug("admin not found");
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public AdminEntity addAdmin(X509Certificate authnCertificate) {

		LOG.debug("add admin");
		AdminEntity admin = new AdminEntity(UUID.randomUUID().toString(),
				authnCertificate.getSubjectX500Principal().toString(),
				authnCertificate.getPublicKey());
		this.entityManager.persist(admin);
		return admin;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAdmin(AdminEntity admin) {

		LOG.debug("remove admin: " + admin.getName());
		AdminEntity attachedAdmin = this.entityManager.find(AdminEntity.class,
				admin.getId());
		this.entityManager.remove(attachedAdmin);
	}
}
