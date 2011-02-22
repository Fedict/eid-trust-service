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

import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.annotations.security.Admin;
import org.jboss.seam.contexts.SessionContext;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.log.Log;

import be.fedict.eid.applet.service.impl.handler.IdentityDataMessageHandler;
import be.fedict.trust.admin.portal.AdminConstants;
import be.fedict.trust.admin.portal.Administrator;
import be.fedict.trust.service.AdministratorService;
import be.fedict.trust.service.entity.AdministratorEntity;
import be.fedict.trust.service.exception.RemoveLastAdminException;

@Stateful
@Name(AdminConstants.ADMIN_SEAM_PREFIX + "admin")
@LocalBinding(jndiBinding = AdminConstants.ADMIN_JNDI_CONTEXT
		+ "AdministratorBean")
public class AdministratorBean implements Administrator {

	private static final String ADMIN_LIST_NAME = AdminConstants.ADMIN_SEAM_PREFIX
			+ "adminList";
	private static final String SELECTED_ADMIN = "selectedAdmin";

	@Logger
	private Log log;

	@EJB
	private AdministratorService administratorService;

	@In
	private SessionContext sessionContext;

	@In
	FacesMessages facesMessages;

	@SuppressWarnings("unused")
	@DataModel(ADMIN_LIST_NAME)
	private List<AdministratorEntity> adminList;

	@DataModelSelection(ADMIN_LIST_NAME)
	@In(value = SELECTED_ADMIN, required = false)
	@Out(value = SELECTED_ADMIN, required = false, scope = ScopeType.PAGE)
	private AdministratorEntity selectedAdmin;

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
	@Factory(ADMIN_LIST_NAME)
	public void adminListFactory() {

		this.log.debug("admin list factory");
		this.adminList = this.administratorService.listAdmins();
	}

	/**
	 * {@inheritDoc}
	 */
    @Admin
	public void register() {

		this.log.debug("register");

		X509Certificate authnCert = (X509Certificate) this.sessionContext
				.get(IdentityDataMessageHandler.AUTHN_CERT_SESSION_ATTRIBUTE);

		this.selectedAdmin = this.administratorService.register(authnCert);
	}

	/**
	 * {@inheritDoc}
	 */
    @Admin
	public String registerPending() {

		this.log.debug("register pending admin");
		this.administratorService.register(this.selectedAdmin);
		adminListFactory();
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public void select() {

		this.log.debug("selected admin: #0", this.selectedAdmin);
	}

	/**
	 * {@inheritDoc}
	 */
    @Admin
	public String remove() {

		this.log.debug("remove administrator");

		try {
			this.administratorService.remove(this.selectedAdmin);
		} catch (RemoveLastAdminException e) {
			this.log.error("cannot remove last administrator");
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR, "errorRemoveLastAdmin");
			return null;
		}

		adminListFactory();
		return "success";
	}
}
