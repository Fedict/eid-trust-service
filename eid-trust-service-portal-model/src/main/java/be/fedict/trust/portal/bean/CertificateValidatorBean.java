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

package be.fedict.trust.portal.bean;

import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.contexts.SessionContext;
import org.jboss.seam.log.Log;

import be.fedict.trust.portal.CertificateValidator;
import be.fedict.trust.service.TrustService;

@Stateless
@Name("certificateValidator")
@LocalBinding(jndiBinding = "fedict/eid/trust/portal/CertificateValidatorBean")
public class CertificateValidatorBean implements CertificateValidator {

	@Logger
	private Log log;

	@In
	private SessionContext sessionContext;

	@EJB
	private TrustService trustService;

	@SuppressWarnings("unused")
	@Out
	private String authnCertStatus;

	public void validate() {
		this.log.debug("validate");
		X509Certificate authnCert = (X509Certificate) this.sessionContext
				.get("eid.certs.authn");
		X509Certificate signCert = (X509Certificate) this.sessionContext
				.get("eid.certs.sign");
		X509Certificate caCert = (X509Certificate) this.sessionContext
				.get("eid.certs.ca");
		X509Certificate rootCert = (X509Certificate) this.sessionContext
				.get("eid.certs.root");

		List<X509Certificate> authnCertChain = new LinkedList<X509Certificate>();
		authnCertChain.add(authnCert);
		authnCertChain.add(caCert);
		authnCertChain.add(rootCert);

		List<X509Certificate> signCertChain = new LinkedList<X509Certificate>();
		signCertChain.add(signCert);
		signCertChain.add(caCert);
		signCertChain.add(rootCert);

		boolean authnCertValid = this.trustService.isValid(authnCertChain);
		if (true == authnCertValid) {
			this.authnCertStatus = "Certificate valid.";
		} else {
			this.authnCertStatus = "Certificate invalid.";
		}
	}
}
