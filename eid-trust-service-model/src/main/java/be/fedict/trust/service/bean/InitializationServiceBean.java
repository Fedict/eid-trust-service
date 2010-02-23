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
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.InitializationService;
import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.entity.TimeProtocol;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.entity.constraints.KeyUsageType;
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
	private ConfigurationDAO configurationDAO;

	@EJB
	private TrustDomainDAO trustDomainDAO;

	@EJB
	private SchedulingService schedulingService;

	public void initialize() {

		LOG.debug("initialize");

		initNetworkConfig();
		initClockDrift();

		List<TrustPointEntity> trustPoints = initBelgianEidTrustPoints();

		initBelgianEidAuthTrustDomain(trustPoints);
		initBelgianEidNonRepudiationDomain(trustPoints);
		initBelgianEidNationalRegistryTrustDomain(trustPoints);

	}

	private void initNetworkConfig() {

		// Default network config
		configurationDAO.setNetworkConfig(null, 0);
		configurationDAO.setNetworkConfigEnabled(false);
	}

	private void initClockDrift() {

		// Default clock drift config
		ClockDriftConfigEntity clockDriftConfig = configurationDAO
				.setClockDriftConfig(TimeProtocol.NTP,
						TrustServiceConstants.CLOCK_DRIFT_NTP_SERVER,
						TrustServiceConstants.CLOCK_DRIFT_TIMEOUT,
						TrustServiceConstants.CLOCK_DRIFT_MAX_CLOCK_OFFSET,
						TrustServiceConstants.DEFAULT_CRON);

		// Clock drift timer
		LOG.debug("start timer for clock drift");
		try {
			this.schedulingService.startTimer(clockDriftConfig, false);
		} catch (InvalidCronExpressionException e) {
			LOG.error("Failed to start timer for clock drift", e);
			throw new RuntimeException(e);
		}

	}

	/**
	 * Initialize the Belgian eID trust points.
	 */
	private List<TrustPointEntity> initBelgianEidTrustPoints() {

		List<TrustPointEntity> trustPoints = new LinkedList<TrustPointEntity>();

		// Belgian eID Root CA trust points
		X509Certificate rootCaCertificate = loadCertificate("be/fedict/trust/belgiumrca.crt");
		CertificateAuthorityEntity rootCa = this.trustDomainDAO
				.findCertificateAuthority(rootCaCertificate);
		if (null == rootCa) {
			rootCa = this.trustDomainDAO
					.addCertificateAuthority(rootCaCertificate);
		}

		if (null == rootCa.getTrustPoint()) {
			TrustPointEntity rootCaTrustPoint = this.trustDomainDAO
					.addTrustPoint(null, rootCa);
			rootCa.setTrustPoint(rootCaTrustPoint);
		}
		trustPoints.add(rootCa.getTrustPoint());

		X509Certificate rootCa2Certificate = loadCertificate("be/fedict/trust/belgiumrca2.crt");
		CertificateAuthorityEntity rootCa2 = this.trustDomainDAO
				.findCertificateAuthority(rootCa2Certificate);
		if (null == rootCa2) {
			rootCa2 = this.trustDomainDAO
					.addCertificateAuthority(rootCa2Certificate);
		}

		if (null == rootCa2.getTrustPoint()) {
			TrustPointEntity rootCa2TrustPoint = this.trustDomainDAO
					.addTrustPoint(null, rootCa2);
			rootCa2.setTrustPoint(rootCa2TrustPoint);
		}
		trustPoints.add(rootCa2.getTrustPoint());
		return trustPoints;
	}

	/**
	 * Initialize the Belgian eID authentication trust domain.
	 */
	private void initBelgianEidAuthTrustDomain(
			List<TrustPointEntity> trustPoints) {

		TrustDomainEntity trustDomain = this.trustDomainDAO
				.findTrustDomain(TrustServiceConstants.BELGIAN_EID_AUTH_TRUST_DOMAIN);
		if (null == trustDomain) {
			LOG.debug("create Belgian eID authentication trust domain");
			trustDomain = this.trustDomainDAO.addTrustDomain(
					TrustServiceConstants.BELGIAN_EID_AUTH_TRUST_DOMAIN,
					TrustServiceConstants.DEFAULT_CRON);
			this.trustDomainDAO.setDefaultTrustDomain(trustDomain);
		}
		trustDomain.setTrustPoints(trustPoints);

		// initialize certificate constraints
		this.trustDomainDAO.addKeyUsageConstraint(trustDomain,
				KeyUsageType.DIGITAL_SIGNATURE_IDX, true);
		this.trustDomainDAO.addKeyUsageConstraint(trustDomain,
				KeyUsageType.NON_REPUDIATION_IDX, false);

		this.trustDomainDAO.addCertificatePolicy(trustDomain,
				"2.16.56.1.1.1.2.2");
		this.trustDomainDAO.addCertificatePolicy(trustDomain,
				"2.16.56.1.1.1.7.2");
		this.trustDomainDAO.addCertificatePolicy(trustDomain,
				"2.16.56.9.1.1.2.2");
		this.trustDomainDAO.addCertificatePolicy(trustDomain,
				"2.16.56.9.1.1.7.2");

		// start timer
		initTrustDomainScheduling(trustDomain);
	}

	private void initBelgianEidNonRepudiationDomain(
			List<TrustPointEntity> trustPoints) {

		TrustDomainEntity trustDomain = this.trustDomainDAO
				.findTrustDomain(TrustServiceConstants.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN);
		if (null == trustDomain) {
			LOG.debug("create Belgian eID Non Repudiation trust domain");
			trustDomain = this.trustDomainDAO
					.addTrustDomain(
							TrustServiceConstants.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN,
							null);
		}
		trustDomain.setTrustPoints(trustPoints);

		// initialize certificate constraints
		this.trustDomainDAO.addKeyUsageConstraint(trustDomain,
				KeyUsageType.DIGITAL_SIGNATURE_IDX, false);
		this.trustDomainDAO.addKeyUsageConstraint(trustDomain,
				KeyUsageType.NON_REPUDIATION_IDX, true);

		this.trustDomainDAO.addCertificatePolicy(trustDomain,
				"2.16.56.1.1.1.2.1");
		this.trustDomainDAO.addCertificatePolicy(trustDomain,
				"2.16.56.1.1.1.7.1");
		this.trustDomainDAO.addCertificatePolicy(trustDomain,
				"2.16.56.9.1.1.2.1");
		this.trustDomainDAO.addCertificatePolicy(trustDomain,
				"2.16.56.9.1.1.7.1");

		this.trustDomainDAO.addQCStatementsConstraint(trustDomain, true);

	}

	private void initBelgianEidNationalRegistryTrustDomain(
			List<TrustPointEntity> trustPoints) {

		TrustDomainEntity trustDomain = this.trustDomainDAO
				.findTrustDomain(TrustServiceConstants.BELGIAN_EID_NATIONAL_REGISTRY_TRUST_DOMAIN);
		if (null == trustDomain) {
			LOG.debug("create Belgian eID national registry trust domain");
			trustDomain = this.trustDomainDAO
					.addTrustDomain(
							TrustServiceConstants.BELGIAN_EID_NATIONAL_REGISTRY_TRUST_DOMAIN,
							null);
		}
		trustDomain.setTrustPoints(trustPoints);

		// initialize certificate constraints
		this.trustDomainDAO.addKeyUsageConstraint(trustDomain,
				KeyUsageType.DIGITAL_SIGNATURE_IDX, true);
		this.trustDomainDAO.addKeyUsageConstraint(trustDomain,
				KeyUsageType.NON_REPUDIATION_IDX, true);

		this.trustDomainDAO
				.addCertificatePolicy(trustDomain, "2.16.56.1.1.1.4");

		this.trustDomainDAO.addDNConstraint(trustDomain, "CN=RRN, O=RRN, C=BE");

	}

	private void initTrustDomainScheduling(TrustDomainEntity trustDomain) {

		// Belgian eID trust domain timer
		LOG.debug("start timer for domain " + trustDomain.getName());
		try {
			this.schedulingService.startTimer(trustDomain, false);
		} catch (InvalidCronExpressionException e) {
			LOG.error("Failed to start timer for domain: "
					+ trustDomain.getName(), e);
			throw new RuntimeException(e);
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
