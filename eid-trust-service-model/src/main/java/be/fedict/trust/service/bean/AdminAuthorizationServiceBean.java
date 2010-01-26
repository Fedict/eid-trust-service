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

package be.fedict.trust.service.bean;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.trust.service.AdminAuthorizationService;
import be.fedict.trust.service.entity.AdminEntity;

/**
 * Administrator Authorization Service Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
@LocalBinding(jndiBinding = AdminAuthorizationService.JNDI_BINDING)
public class AdminAuthorizationServiceBean implements AdminAuthorizationService {

	private static final Log LOG = LogFactory
			.getLog(AdminAuthorizationServiceBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	public String authenticate(List<X509Certificate> authnCertChain)
			throws NoSuchAlgorithmException, InvalidKeySpecException,
			CertPathValidatorException {

		LOG.debug("authenticate: "
				+ authnCertChain.get(0).getSubjectX500Principal().getName());

		// validate
		if (authnCertChain.size() < 2) {
			LOG.error("no root certificate found");
			return null;
		}
		try {
			authnCertChain.get(0).verify(authnCertChain.get(1).getPublicKey());
		} catch (Exception e) {
			LOG.error("verification error: " + e.getMessage());
			throw new CertPathValidatorException(e);
		}

		// lookup admin entity
		List<AdminEntity> admins = listAdmins();
		if (null == admins || admins.isEmpty()) {
			// no administrator yet, register
			LOG.debug("register initial administrator");
			AdminEntity admin = new AdminEntity(UUID.randomUUID().toString(),
					authnCertChain.get(0).getPublicKey());
			this.entityManager.persist(admin);
			return admin.getId();
		}

		for (AdminEntity admin : listAdmins()) {
			if (admin.getPublicKey().equals(
					authnCertChain.get(0).getPublicKey())) {
				LOG.debug("found admin: " + admin.getId());
				return admin.getId();
			}
		}

		LOG.error("administrator not found");
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<AdminEntity> listAdmins() {
		Query query = this.entityManager.createQuery("FROM AdminEntity");
		return (List<AdminEntity>) query.getResultList();
	}
}
