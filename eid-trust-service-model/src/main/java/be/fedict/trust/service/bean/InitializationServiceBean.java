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

package be.fedict.trust.service.bean;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.crl.CrlTrustLinker;
import be.fedict.trust.service.InitializationService;
import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;

/**
 * Initialization Service Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
public class InitializationServiceBean implements InitializationService {

	private static final Log LOG = LogFactory
			.getLog(InitializationServiceBean.class);

	@EJB
	private TrustDomainDAO trustDomainDAO;

	@EJB
	private SchedulingService schedulingService;

	public void initialize() {

		LOG.debug("initialize");

		// Belgian eID trust domain
		TrustDomainEntity beidTrustDomain = this.trustDomainDAO
				.findTrustDomain(TrustServiceConstants.BELGIAN_EID_TRUST_DOMAIN);
		if (null == beidTrustDomain) {
			LOG.debug("create Belgian eID trust domain");
			beidTrustDomain = this.trustDomainDAO.addTrustDomain(
					TrustServiceConstants.BELGIAN_EID_TRUST_DOMAIN,
					TrustServiceConstants.DEFAULT_CRON);
		}

		// add Root CA trust points
		X509Certificate rootCaCertificate = loadCertificate("be/fedict/trust/belgiumrca.crt");
		CertificateAuthorityEntity rootCa = this.trustDomainDAO
				.addCertificateAuthority(getCrlUrl(rootCaCertificate),
						rootCaCertificate, null);

		TrustPointEntity rootCaTrustPoint = this.trustDomainDAO
				.findTrustPoint(TrustServiceConstants.BELGIAN_EID_ROOT_CA_TRUST_POINT);
		if (null == rootCaTrustPoint) {
			rootCaTrustPoint = this.trustDomainDAO.addTrustPoint(
					TrustServiceConstants.BELGIAN_EID_ROOT_CA_TRUST_POINT,
					null, beidTrustDomain, rootCa);
		}
		rootCa.setTrustPoint(rootCaTrustPoint);

		X509Certificate rootCa2Certificate = loadCertificate("be/fedict/trust/belgiumrca2.crt");
		CertificateAuthorityEntity rootCa2 = this.trustDomainDAO
				.addCertificateAuthority(getCrlUrl(rootCa2Certificate),
						rootCa2Certificate, null);

		TrustPointEntity rootCa2TrustPoint = this.trustDomainDAO
				.findTrustPoint(TrustServiceConstants.BELGIAN_EID_ROOT_CA2_TRUST_POINT);
		if (null == rootCa2TrustPoint) {
			rootCa2TrustPoint = this.trustDomainDAO.addTrustPoint(
					TrustServiceConstants.BELGIAN_EID_ROOT_CA2_TRUST_POINT,
					null, beidTrustDomain, rootCa2);
		}
		rootCa2.setTrustPoint(rootCa2TrustPoint);

		// Start default scheduling timer
		LOG.debug("start timer for domain " + beidTrustDomain.getName());
		try {
			this.schedulingService.startTimer(beidTrustDomain);
		} catch (InvalidCronExpressionException e) {
			LOG.error("Failed to start timer for domain: "
					+ beidTrustDomain.getName(), e);
			throw new RuntimeException(e);
		}
	}

	private String getCrlUrl(X509Certificate certificate) {

		URI crlUri = CrlTrustLinker.getCrlUri(certificate);
		if (null == crlUri)
			return null;
		try {
			return crlUri.toURL().toString();
		} catch (MalformedURLException e) {
			LOG.warn("malformed URL: " + e.getMessage(), e);
			return null;
		}
	}

	private static X509Certificate loadCertificate(String resourceName) {
		LOG.debug("loading certificate: " + resourceName);
		Thread currentThread = Thread.currentThread();
		ClassLoader classLoader = currentThread.getContextClassLoader();
		InputStream certificateInputStream = classLoader
				.getResourceAsStream(resourceName);
		if (null == certificateInputStream) {
			throw new IllegalArgumentException("resource not found: "
					+ resourceName);
		}
		try {
			CertificateFactory certificateFactory = CertificateFactory
					.getInstance("X.509");
			X509Certificate certificate = (X509Certificate) certificateFactory
					.generateCertificate(certificateInputStream);
			return certificate;
		} catch (CertificateException e) {
			throw new RuntimeException("X509 error: " + e.getMessage(), e);
		}
	}

}
