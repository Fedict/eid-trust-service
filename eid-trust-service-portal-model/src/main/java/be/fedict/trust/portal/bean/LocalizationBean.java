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

package be.fedict.trust.portal.bean;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.log.Log;

import be.fedict.trust.portal.Localization;
import be.fedict.trust.portal.PortalConstants;
import be.fedict.trust.service.LocalizationService;

@Stateless
@Name(PortalConstants.PORTAL_SEAM_PREFIX + "localization")
@LocalBinding(jndiBinding = PortalConstants.PORTAL_JNDI_CONTEXT
		+ "LocalizationBean")
public class LocalizationBean implements Localization {

	@Logger
	private Log log;

	@EJB
	private LocalizationService localizationService;

	public String getInfo() {

		this.log.debug("get \"info\" localization");
		return this.localizationService.findText("info", LocaleSelector
				.instance().getLocale());
	}

}
