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
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.log.Log;

import be.fedict.trust.admin.portal.TrustDomain;
import be.fedict.trust.service.TrustDomainService;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;

@Stateful
@Name("trustDomain")
@LocalBinding(jndiBinding = "fedict/eid/trust/admin/portal/TrustDomainBean")
public class TrustDomainBean implements TrustDomain {

	public static final String SELECTED_TRUST_DOMAIN = "selectedTrustDomain";
	private static final String TRUST_DOMAIN_LIST_NAME = "trustDomainList";

	@Logger
	private Log log;

	@EJB
	private TrustDomainService trustDomainService;

	@In(create = true)
	FacesMessages facesMessages;

	@SuppressWarnings("unused")
	@DataModel(TRUST_DOMAIN_LIST_NAME)
	private List<TrustDomainEntity> trustDomainList;

	@DataModelSelection(TRUST_DOMAIN_LIST_NAME)
	@Out(value = SELECTED_TRUST_DOMAIN, required = false, scope = ScopeType.SESSION)
	@In(required = false)
	private TrustDomainEntity selectedTrustDomain;

	/**
	 * {@inheritDoc}
	 */
	@Remove
	@Destroy
	public void destroyCallback() {

		log.debug("#destroy");
	}

	/**
	 * {@inheritDoc}
	 */
	@Factory(TRUST_DOMAIN_LIST_NAME)
	public void trustDomainListFactory() {

		log.debug("trust domain list factory");
		trustDomainList = trustDomainService.listTrustDomains();
	}

	/**
	 * {@inheritDoc}
	 */
	public String modify() {

		log.debug("modify: " + selectedTrustDomain.getName());
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String save() {

		log.debug("save: " + selectedTrustDomain.getName());
		try {
			trustDomainService.save(selectedTrustDomain);
		} catch (InvalidCronExpressionException e) {
			facesMessages.addToControlFromResourceBundle("cron",
					StatusMessage.Severity.ERROR, "errorCronExpressionInvalid");
			return null;
		}
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	public String cancel() {

		log.debug("cancel: " + selectedTrustDomain.getName());
		return "cancel";
	}

}
