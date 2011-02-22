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

package be.fedict.trust.admin.portal.bean;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.security.Admin;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;

import be.fedict.trust.admin.portal.AdminConstants;
import be.fedict.trust.admin.portal.Audit;
import be.fedict.trust.service.AuditService;
import be.fedict.trust.service.entity.AuditEntity;

@Stateful
@Name(AdminConstants.ADMIN_SEAM_PREFIX + "audit")
@LocalBinding(jndiBinding = AdminConstants.ADMIN_JNDI_CONTEXT + "AuditBean")
public class AuditBean implements Audit {

	private static final String AUDIT_LIST_NAME = AdminConstants.ADMIN_SEAM_PREFIX
			+ "auditList";

	@Logger
	private Log log;

	@EJB
	private AuditService auditService;

	@In
	FacesMessages facesMessages;

	@SuppressWarnings("unused")
	@DataModel(AUDIT_LIST_NAME)
	private List<AuditEntity> auditList;

	/**
	 * {@inheritDoc}
	 */
	@Remove
	@Destroy
	public void destroyCallback() {

		this.log.debug("#destroy");
	}

	/**
	 * {@inheritDoc}
	 */
	@Factory(AUDIT_LIST_NAME)
    @Admin
	public void auditListFactory() {

		this.log.debug("audit list factory");
		this.auditList = this.auditService.listAudits();
	}

	/**
	 * {@inheritDoc}
	 */
    @Admin
	public String clear() {

		this.log.debug("clear audit logs");
		this.auditService.clearAudits();
		auditListFactory();
		return "success";
	}
}
