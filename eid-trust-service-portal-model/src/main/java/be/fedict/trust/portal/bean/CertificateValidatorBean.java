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
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.contexts.SessionContext;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.log.Log;

import be.fedict.eid.applet.service.impl.handler.IdentityDataMessageHandler;
import be.fedict.trust.TrustLinkerResult;
import be.fedict.trust.client.TrustServiceDomains;
import be.fedict.trust.portal.CertificateValidator;
import be.fedict.trust.portal.PortalConstants;
import be.fedict.trust.service.TrustService;
import be.fedict.trust.service.ValidationResult;
import be.fedict.trust.service.exception.TrustDomainNotFoundException;

@Stateful
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

	private ValidationResult authnResult;
	private ValidationResult signResult;

	/**
	 * {@inheritDoc}
	 */
	@Remove
	public String validateCertificates() {

		this.authnResult = null;
		this.signResult = null;
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

		if (null == authnCert) {
			this.log.error("no eID certificates in session");
			this.authnResult = new ValidationResult(
					new TrustLinkerResult(false), null);
			this.signResult = new ValidationResult(
					new TrustLinkerResult(false), null);
			return;
		}

		/*
		 * Validate authentication certificate chain
		 */
		List<X509Certificate> authnCertChain = new LinkedList<X509Certificate>();
		authnCertChain.add(authnCert);
		authnCertChain.add(caCert);
		authnCertChain.add(rootCert);

		/*
		 * Validate signing certificate chain
		 */
		List<X509Certificate> signCertChain = new LinkedList<X509Certificate>();
		signCertChain.add(signCert);
		signCertChain.add(caCert);
		signCertChain.add(rootCert);

		try {
			this.authnResult = this.trustService.validate(
					TrustServiceDomains.BELGIAN_EID_AUTH_TRUST_DOMAIN,
					authnCertChain, false);
			this.signResult = this.trustService
					.validate(
							TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
							signCertChain, false);
		} catch (TrustDomainNotFoundException e) {
			this.log.error(
					"error validating eID certificates: " + e.getMessage(), e);
			this.facesMessages.addFromResourceBundle(
					StatusMessage.Severity.ERROR, "errorTrustDomainNotFound");
			this.authnResult = new ValidationResult(
					new TrustLinkerResult(false), null);
			this.signResult = new ValidationResult(
					new TrustLinkerResult(false), null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isAuthnValid() {

		return this.authnResult.isValid();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isSigningValid() {

		return this.signResult.isValid();
	}

	public TrustLinkerResult getAuthnResult() {

		return this.authnResult.getResult();
	}

	public TrustLinkerResult getSigningResult() {

		return this.signResult.getResult();
	}
}
