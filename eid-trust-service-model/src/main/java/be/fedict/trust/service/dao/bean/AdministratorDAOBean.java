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
import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.trust.service.dao.AdministratorDAO;
import be.fedict.trust.service.entity.AdminEntity;

/**
 * Administrator DAO Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
@LocalBinding(jndiBinding = AdministratorDAO.JNDI_BINDING)
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
		Query query = this.entityManager.createQuery("FROM AdminEntity");
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
				getName(authnCertificate.getSubjectX500Principal()),
				authnCertificate.getPublicKey());
		this.entityManager.persist(admin);
		return admin;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeAdmin(AdminEntity admin) {

		LOG.debug("remove admin: " + admin.getName());
		this.entityManager.remove(admin);
	}

	private String getName(X500Principal x500Principal) {

		return x500Principal.getName().substring(
				x500Principal.getName().indexOf("CN=") + 3,
				x500Principal.getName().indexOf("(Authentication)"));
	}

}
