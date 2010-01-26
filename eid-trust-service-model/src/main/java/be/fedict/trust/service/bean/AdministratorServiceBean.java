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

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.ejb3.annotation.SecurityDomain;

import be.fedict.trust.service.AdministratorService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.entity.AdminEntity;

/**
 * Administrator Service Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
@LocalBinding(jndiBinding = AdministratorService.JNDI_BINDING)
@SecurityDomain(TrustServiceConstants.ADMIN_SECURITY_DOMAIN)
public class AdministratorServiceBean implements AdministratorService {

	private static final Log LOG = LogFactory
			.getLog(AdministratorServiceBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	@SuppressWarnings("unchecked")
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public List<AdminEntity> listAdmins() {

		LOG.debug("list admins");
		Query query = this.entityManager.createQuery("FROM AdminEntity");
		return (List<AdminEntity>) query.getResultList();
	}

}
