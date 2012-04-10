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

package be.fedict.trust.admin.portal.bean;

import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

import be.fedict.trust.admin.portal.AdminConstants;
import be.fedict.trust.admin.portal.MenuController;

@Stateful
@Name("ts_menuController")
@LocalBinding(jndiBinding = AdminConstants.ADMIN_JNDI_CONTEXT
		+ "MenuControllerBean")
@Scope(ScopeType.SESSION)
public class MenuControllerBean implements MenuController {

	@Logger
	private Log log;

	private String selectedChild;

	public String getSelectedChild() {
		return this.selectedChild;
	}

	public void setSelectedChild(String selectedChild) {
		this.selectedChild = selectedChild;
	}

	@Remove
	@Destroy
	public void destroy() {
		this.log.debug("destroy");
	}
}
