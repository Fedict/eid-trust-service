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

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.jms.JMSException;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.annotations.security.Admin;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.log.Log;

import be.fedict.trust.admin.portal.AdminConstants;
import be.fedict.trust.admin.portal.CertificateAuthority;
import be.fedict.trust.service.TrustDomainService;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;

@Stateful
@Name(AdminConstants.ADMIN_SEAM_PREFIX + "certificateAuthority")
@LocalBinding(jndiBinding = AdminConstants.ADMIN_JNDI_CONTEXT
		+ "CertificateAuthorityBean")
public class CertificateAuthorityBean implements CertificateAuthority {

	private static final String CERTIFICATE_AUTHORITY_LIST_NAME = AdminConstants.ADMIN_SEAM_PREFIX
			+ "caList";

	@Logger
	private Log log;

	@EJB
	private TrustDomainService trustDomainService;

	@In
	FacesMessages facesMessages;

	private String crlUrl;

	private String certUrl;

	private String crlText;

	@DataModel(CERTIFICATE_AUTHORITY_LIST_NAME)
	@SuppressWarnings("unused")
	private List<CertificateAuthorityEntity> caList;

	@DataModelSelection(CERTIFICATE_AUTHORITY_LIST_NAME)
	private CertificateAuthorityEntity selectedCA;

	@Factory(CERTIFICATE_AUTHORITY_LIST_NAME)
	public void certificateAuthorityListFactory() {
		this.caList = this.trustDomainService.listCAs();
	}

	@Remove
	@Destroy
	public void destroyCallback() {
		this.log.debug("#destroy");
	}

	@Admin
	public String refresh() {
		this.log.debug("refresh CA cache: #0", this.selectedCA.getName());
		try {
			this.trustDomainService.refreshCACache(this.selectedCA);
		} catch (JMSException e) {
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR, "errorHarvesterNotification");
			return null;
		}
		return "success";
	}

	@Admin
	public long getCachedCertificates() {
		return this.trustDomainService.getTotalCachedCertificates();
	}

	@Admin
	public long getCachedCAs() {
		return this.trustDomainService.getTotalActiveCachedCAs();
	}

	public String getCrlUrl() {
		return this.crlUrl;
	}

	public void setCrlUrl(String crlUrl) {
		this.crlUrl = crlUrl;
	}

	public String getCertUrl() {
		return this.certUrl;
	}

	public void setCertUrl(String certUrl) {
		this.certUrl = certUrl;
	}

	public String coldStart() {
		this.log.debug("cold start: #0 #1", this.crlUrl, this.certUrl);
		if (null != this.crlText && false == this.crlText.isEmpty()) {
			this.trustDomainService.coldStart(this.crlText);
		} else {
			this.trustDomainService.coldStart(this.crlUrl, this.certUrl);
		}
		this.crlUrl = null;
		this.certUrl = null;
		this.crlText = null;
		return "success";
	}

	public String getCrlText() {
		return this.crlText;
	}

	public void setCrlText(String crlText) {
		this.crlText = crlText;
	}

	public String remove() {
		this.log.debug("remove CA cache: #0", this.selectedCA.getName());
		try {
			this.trustDomainService.removeCACache(this.selectedCA);
		} catch (JMSException e) {
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR, "errorHarvesterNotification");
			return null;
		}
		return "success";
	}
}
