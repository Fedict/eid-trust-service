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

package be.fedict.trust.admin.portal.bean;

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
import org.jboss.seam.log.Log;

import be.fedict.trust.admin.portal.Administrator;
import be.fedict.trust.service.AdministratorService;
import be.fedict.trust.service.entity.AdminEntity;

@Stateful
@Name("admin")
@LocalBinding(jndiBinding = "fedict/eid/trust/admin/portal/AdministratorBean")
public class AdministratorBean implements Administrator {

	private static final String ADMIN_LIST_NAME = "adminList";

	@Logger
	private Log log;

	@EJB
	private AdministratorService administratorService;

	@SuppressWarnings("unused")
	@DataModel(ADMIN_LIST_NAME)
	private List<AdminEntity> adminList;

	@DataModelSelection(ADMIN_LIST_NAME)
	@Out(value = "selectedAdmin", required = false, scope = ScopeType.SESSION)
	@In(required = false)
	private AdminEntity selectedAdmin;

	@Remove
	@Destroy
	public void destroyCallback() {

		log.debug("#destroy");
		selectedAdmin = null;
	}

	@Factory(ADMIN_LIST_NAME)
	public void adminListFactory() {

		log.debug("admin list factory");
		adminList = administratorService.listAdmins();
	}

}
