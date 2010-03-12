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

import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.contexts.SessionContext;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.log.Log;

import be.fedict.eid.applet.service.impl.handler.IdentityDataMessageHandler;
import be.fedict.trust.portal.CertificateValidator;
import be.fedict.trust.portal.PortalConstants;
import be.fedict.trust.service.TrustService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.ValidationResult;
import be.fedict.trust.service.exception.TrustDomainNotFoundException;

@Stateless
@Name(PortalConstants.PORTAL_SEAM_PREFIX + "certificateValidator")
@LocalBinding(jndiBinding = PortalConstants.PORTAL_JNDI_CONTEXT
		+ "CertificateValidatorBean")
public class CertificateValidatorBean implements CertificateValidator {

	@Logger
	private Log log;

	@In
	private SessionContext sessionContext;

	@In
	FacesMessages facesMessages;

	@EJB
	private TrustService trustService;

	@Out(scope = ScopeType.SESSION)
	private boolean authn = true;

	@SuppressWarnings("unused")
	@Out(required = false)
	private String authnCertStatus;

	/**
	 * {@inheritDoc}
	 */
	public String validateAuthn() {

		this.log.debug("validate authn");
		this.authn = true;
		return "validate";
	}

	/**
	 * {@inheritDoc}
	 */
	public String validateSigning() {

		this.log.debug("validate signing");
		this.authn = false;
		return "validate";
	}

	/**
	 * {@inheritDoc}
	 */
	public void validate() {
		this.log.debug("validate");

		X509Certificate authnCert = (X509Certificate) this.sessionContext
				.get(IdentityDataMessageHandler.AUTHN_CERT_SESSION_ATTRIBUTE);
		X509Certificate signCert = (X509Certificate) this.sessionContext
				.get(IdentityDataMessageHandler.SIGN_CERT_SESSION_ATTRIBUTE);
		X509Certificate caCert = (X509Certificate) this.sessionContext
				.get(IdentityDataMessageHandler.CA_CERT_SESSION_ATTRIBUTE);
		X509Certificate rootCert = (X509Certificate) this.sessionContext
				.get(IdentityDataMessageHandler.ROOT_CERT_SESSION_ATTRIBTUE);

		List<X509Certificate> certChain = new LinkedList<X509Certificate>();
		if (this.authn)
			certChain.add(authnCert);
		else
			certChain.add(signCert);
		certChain.add(caCert);
		certChain.add(rootCert);

		String trustDomain = this.authn ? TrustServiceConstants.BELGIAN_EID_AUTH_TRUST_DOMAIN
				: TrustServiceConstants.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN;

		ValidationResult validationResult;
		try {
			validationResult = this.trustService.validate(trustDomain,
					certChain, false);
		} catch (TrustDomainNotFoundException e) {
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR, "errorTrustDomainNotFound");
			return;
		}
		if (validationResult.isValid()) {
			this.authnCertStatus = "Certificate valid.";
		} else {
			this.authnCertStatus = "Certificate invalid.";
		}
	}
}
