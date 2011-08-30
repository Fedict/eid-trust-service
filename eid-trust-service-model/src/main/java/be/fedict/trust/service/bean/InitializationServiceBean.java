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

package be.fedict.trust.service.bean;

import be.fedict.trust.client.TrustServiceDomains;
import be.fedict.trust.service.InitializationService;
import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.SnmpConstants;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.dao.CertificateAuthorityDAO;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.dao.LocalizationDAO;
import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.*;
import be.fedict.trust.service.entity.constraints.KeyUsageType;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.snmp.SNMPInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Initialization Service Bean implementation.
 * 
 * @author wvdhaute
 */
@Stateless
public class InitializationServiceBean implements InitializationService {

	private static final Log LOG = LogFactory
			.getLog(InitializationServiceBean.class);

	@EJB
	private ConfigurationDAO configurationDAO;

	@EJB
	private LocalizationDAO localizationDAO;

	@EJB
	private TrustDomainDAO trustDomainDAO;

	@EJB
	private CertificateAuthorityDAO certificateAuthorityDAO;

	@EJB
	private SchedulingService schedulingService;

	public void initialize() {

		LOG.debug("initialize");

		initTexts();

		initWSSecurityConfig();
		initNetworkConfig();
		initClockDrift();

		initSnmpCounters();

		if (this.trustDomainDAO.listTrustDomains().isEmpty()) {
			List<TrustPointEntity> trustPoints = initBelgianEidTrustPoints();

			initBelgianEidAuthTrustDomain(trustPoints);
			initBelgianEidNonRepudiationDomain(trustPoints);
			initBelgianEidNationalRegistryTrustDomain(trustPoints);
			initBelgianEidTestCardsTrustDomain();

			initBelgianTSATrustDomain();
		}

		initTimers();
	}

	private void initSnmpCounters() {

		SNMPInterceptor.setValue(SnmpConstants.VALIDATE,
				SnmpConstants.SNMP_SERVICE, 0L);
		SNMPInterceptor.setValue(SnmpConstants.VALIDATE_TSA,
				SnmpConstants.SNMP_SERVICE, 0L);
		SNMPInterceptor.setValue(SnmpConstants.VALIDATE_ATTRIBUTE_CERT,
				SnmpConstants.SNMP_SERVICE, 0L);
		SNMPInterceptor.setValue(SnmpConstants.CACHE_REFRESH,
				SnmpConstants.SNMP_SERVICE, 0L);
		SNMPInterceptor.setValue(SnmpConstants.CACHE_HITS,
				SnmpConstants.SNMP_SERVICE, 0L);
		SNMPInterceptor.setValue(SnmpConstants.CACHE_MISSES,
				SnmpConstants.SNMP_SERVICE, 0L);
		SNMPInterceptor.setValue(SnmpConstants.CACHE_HIT_PERCENTAGE,
				SnmpConstants.SNMP_SERVICE, 0L);
		SNMPInterceptor.setValue(SnmpConstants.CRL_DOWNLOAD_FAILURES,
				SnmpConstants.SNMP_SERVICE, 0L);
		SNMPInterceptor.setValue(SnmpConstants.OCSP_FAILURES,
				SnmpConstants.SNMP_SERVICE, 0L);
	}

	private void initTexts() {

		if (null == this.localizationDAO
				.findLocalization(TrustServiceConstants.INFO_MESSAGE_KEY)) {
			Map<Locale, String> texts = new HashMap<Locale, String>();
			texts.put(Locale.ENGLISH, "");
			texts.put(new Locale("nl"), "");
			texts.put(Locale.FRENCH, "");
			texts.put(Locale.GERMAN, "");
			this.localizationDAO.addLocalization(
					TrustServiceConstants.INFO_MESSAGE_KEY, texts);
		}
	}

	private void initWSSecurityConfig() {

		// Default WS Security config
		if (null == this.configurationDAO.findWSSecurityConfig()) {
			this.configurationDAO.setWSSecurityConfig(false,
					KeyStoreType.PKCS12, null, null, null, null);
		}
	}

	private void initNetworkConfig() {

		// Default network config
		if (null == this.configurationDAO.findNetworkConfigEntity()) {
			this.configurationDAO.setNetworkConfig(null, 0);
			this.configurationDAO.setNetworkConfigEnabled(false);
		}
	}

	private void initClockDrift() {

		// Default clock drift config
		ClockDriftConfigEntity clockDriftConfig = this.configurationDAO
				.findClockDriftConfig();
		if (null == clockDriftConfig) {
			this.configurationDAO.setClockDriftConfig(TimeProtocol.NTP,
					TrustServiceConstants.CLOCK_DRIFT_NTP_SERVER,
					TrustServiceConstants.CLOCK_DRIFT_TIMEOUT,
					TrustServiceConstants.CLOCK_DRIFT_MAX_CLOCK_OFFSET,
					TrustServiceConstants.DEFAULT_CRON_EXPRESSION);
		}
	}

	/*
	 * Initialize the Belgian eID trust points.
	 */

	private List<TrustPointEntity> initBelgianEidTrustPoints() {

		List<TrustPointEntity> trustPoints = new LinkedList<TrustPointEntity>();

		// Belgian eID Root CA trust points
		X509Certificate rootCaCertificate = loadCertificate("be/fedict/trust/belgiumrca.crt");
		CertificateAuthorityEntity rootCa = this.certificateAuthorityDAO
				.findCertificateAuthority(rootCaCertificate);
		if (null == rootCa) {
			rootCa = this.certificateAuthorityDAO.addCertificateAuthority(
					rootCaCertificate, "http://crl.eid.belgium.be/belgium.crl");
		}

		if (null == rootCa.getTrustPoint()) {
			TrustPointEntity rootCaTrustPoint = this.trustDomainDAO
					.addTrustPoint(
							TrustServiceConstants.DEFAULT_CRON_EXPRESSION,
							rootCa);
			rootCa.setTrustPoint(rootCaTrustPoint);
		}
		trustPoints.add(rootCa.getTrustPoint());

		X509Certificate rootCa2Certificate = loadCertificate("be/fedict/trust/belgiumrca2.crt");
		CertificateAuthorityEntity rootCa2 = this.certificateAuthorityDAO
				.findCertificateAuthority(rootCa2Certificate);
		if (null == rootCa2) {
			rootCa2 = this.certificateAuthorityDAO.addCertificateAuthority(
					rootCa2Certificate,
					"http://crl.eid.belgium.be/belgium2.crl");
		}

		if (null == rootCa2.getTrustPoint()) {
			TrustPointEntity rootCa2TrustPoint = this.trustDomainDAO
					.addTrustPoint(
							TrustServiceConstants.DEFAULT_CRON_EXPRESSION,
							rootCa2);
			rootCa2.setTrustPoint(rootCa2TrustPoint);
		}
		trustPoints.add(rootCa2.getTrustPoint());

		return trustPoints;
	}

	/*
	 * Initialize the Belgian eID authentication trust domain.
	 */

	private void initBelgianEidTestCardsTrustDomain() {

		List<TrustPointEntity> trustPoints = new LinkedList<TrustPointEntity>();

		// Belgian Test Root CA trust point
		X509Certificate rootCertificate = loadCertificate("be/fedict/trust/belgiumtestrca.crt");
		CertificateAuthorityEntity rootCa = this.certificateAuthorityDAO
				.findCertificateAuthority(rootCertificate);
		if (null == rootCa) {
			rootCa = this.certificateAuthorityDAO.addCertificateAuthority(
					rootCertificate, null);
		}

		if (null == rootCa.getTrustPoint()) {
			TrustPointEntity rootCaTrustPoint = this.trustDomainDAO
					.addTrustPoint(
							TrustServiceConstants.DEFAULT_CRON_EXPRESSION,
							rootCa);
			rootCa.setTrustPoint(rootCaTrustPoint);
		}
		trustPoints.add(rootCa.getTrustPoint());

		TrustDomainEntity trustDomain = this.trustDomainDAO
				.findTrustDomain(TrustServiceDomains.BELGIAN_EID_TEST_TRUST_DOMAIN);
		if (null == trustDomain) {
			LOG.debug("create Belgian eID TEST trust domain");
			trustDomain = this.trustDomainDAO
					.addTrustDomain(TrustServiceDomains.BELGIAN_EID_TEST_TRUST_DOMAIN);
		}

		trustDomain.setTrustPoints(trustPoints);

		// initialize certificate constraints
		if (trustDomain.getCertificateConstraints().isEmpty()) {

			this.trustDomainDAO.addCertificatePolicy(trustDomain,
					"2.16.56.1.40.40.40.1");
		}
	}

	private void initBelgianEidAuthTrustDomain(
			List<TrustPointEntity> trustPoints) {

		TrustDomainEntity trustDomain = this.trustDomainDAO
				.findTrustDomain(TrustServiceDomains.BELGIAN_EID_AUTH_TRUST_DOMAIN);
		if (null == trustDomain) {
			LOG.debug("create Belgian eID authentication trust domain");
			trustDomain = this.trustDomainDAO
					.addTrustDomain(TrustServiceDomains.BELGIAN_EID_AUTH_TRUST_DOMAIN);
			this.trustDomainDAO.setDefaultTrustDomain(trustDomain);
		}
		trustDomain.setTrustPoints(trustPoints);

		// initialize certificate constraints
		if (trustDomain.getCertificateConstraints().isEmpty()) {
			this.trustDomainDAO.addKeyUsageConstraint(trustDomain,
					KeyUsageType.DIGITAL_SIGNATURE, true);
			this.trustDomainDAO.addKeyUsageConstraint(trustDomain,
					KeyUsageType.NON_REPUDIATION, false);

			this.trustDomainDAO.addCertificatePolicy(trustDomain,
					"2.16.56.1.1.1.2.2");
			this.trustDomainDAO.addCertificatePolicy(trustDomain,
					"2.16.56.1.1.1.7.2");
			this.trustDomainDAO.addCertificatePolicy(trustDomain,
					"2.16.56.9.1.1.2.2");
			this.trustDomainDAO.addCertificatePolicy(trustDomain,
					"2.16.56.9.1.1.7.2");
		}
	}

	private void initBelgianEidNonRepudiationDomain(
			List<TrustPointEntity> trustPoints) {

		TrustDomainEntity trustDomain = this.trustDomainDAO
				.findTrustDomain(TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN);
		if (null == trustDomain) {
			LOG.debug("create Belgian eID Non Repudiation trust domain");
			trustDomain = this.trustDomainDAO
					.addTrustDomain(TrustServiceDomains.BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN);
		}
		trustDomain.setTrustPoints(trustPoints);

		// initialize certificate constraints
		if (trustDomain.getCertificateConstraints().isEmpty()) {
			this.trustDomainDAO.addKeyUsageConstraint(trustDomain,
					KeyUsageType.DIGITAL_SIGNATURE, false);
			this.trustDomainDAO.addKeyUsageConstraint(trustDomain,
					KeyUsageType.NON_REPUDIATION, true);

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
	}

	private void initBelgianEidNationalRegistryTrustDomain(
			List<TrustPointEntity> trustPoints) {

		TrustDomainEntity trustDomain = this.trustDomainDAO
				.findTrustDomain(TrustServiceDomains.BELGIAN_EID_NATIONAL_REGISTRY_TRUST_DOMAIN);
		if (null == trustDomain) {
			LOG.debug("create Belgian eID national registry trust domain");
			trustDomain = this.trustDomainDAO
					.addTrustDomain(TrustServiceDomains.BELGIAN_EID_NATIONAL_REGISTRY_TRUST_DOMAIN);
		}
		trustDomain.setTrustPoints(trustPoints);

		// initialize certificate constraints
		if (trustDomain.getCertificateConstraints().isEmpty()) {
			this.trustDomainDAO.addKeyUsageConstraint(trustDomain,
					KeyUsageType.DIGITAL_SIGNATURE, true);
			this.trustDomainDAO.addKeyUsageConstraint(trustDomain,
					KeyUsageType.NON_REPUDIATION, true);

			this.trustDomainDAO.addCertificatePolicy(trustDomain,
					"2.16.56.1.1.1.4");
			this.trustDomainDAO.addCertificatePolicy(trustDomain,
					"2.16.56.9.1.1.4");

			this.trustDomainDAO.addDNConstraint(trustDomain,
					"CN=RRN, O=RRN, C=BE");
		}
	}

	/**
	 * Initialize the Belgian TSA trust points.
	 */
	private void initBelgianTSATrustDomain() {

		List<TrustPointEntity> trustPoints = new LinkedList<TrustPointEntity>();

		// Belgian TSA Root CA trust points
		X509Certificate rootCertificate = loadCertificate("be/fedict/trust/belgiumtsa.crt");
		CertificateAuthorityEntity rootCa = this.certificateAuthorityDAO
				.findCertificateAuthority(rootCertificate);
		if (null == rootCa) {
			rootCa = this.certificateAuthorityDAO.addCertificateAuthority(
					rootCertificate, null);
		}

		if (null == rootCa.getTrustPoint()) {
			TrustPointEntity rootCaTrustPoint = this.trustDomainDAO
					.addTrustPoint(
							TrustServiceConstants.DEFAULT_CRON_EXPRESSION,
							rootCa);
			rootCa.setTrustPoint(rootCaTrustPoint);
		}
		trustPoints.add(rootCa.getTrustPoint());

		// Belgian TSA trust domain
		TrustDomainEntity trustDomain = this.trustDomainDAO
				.findTrustDomain(TrustServiceDomains.BELGIAN_TSA_TRUST_DOMAIN);
		if (null == trustDomain) {
			LOG.debug("create Belgian TSA Repudiation trust domain");
			trustDomain = this.trustDomainDAO
					.addTrustDomain(TrustServiceDomains.BELGIAN_TSA_TRUST_DOMAIN);
		}
		trustDomain.setTrustPoints(trustPoints);

		// Add TSA certificate constraint
		this.trustDomainDAO.addTSAConstraint(trustDomain);
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
			return (X509Certificate) certificateFactory
					.generateCertificate(certificateInputStream);
		} catch (CertificateException e) {
			throw new RuntimeException("X509 error: " + e.getMessage(), e);
		}
	}

	/**
	 * Initialize timers for all trust points found and the clock drift config
	 * if enabled.
	 */
	private void initTimers() {

		for (TrustPointEntity trustPoint : this.trustDomainDAO
				.listTrustPoints()) {
			try {
				this.schedulingService.startTimer(trustPoint);
			} catch (InvalidCronExpressionException e) {
				throw new RuntimeException(String.format(
						"Failed to start timer for trustpoint \"%s\"",
						trustPoint.getName()), e);
			}
		}

		ClockDriftConfigEntity clockDriftConfig = this.configurationDAO
				.findClockDriftConfig();
		if (null != clockDriftConfig && clockDriftConfig.isEnabled()) {
			try {
				this.schedulingService.startTimer(clockDriftConfig);
			} catch (InvalidCronExpressionException e) {
				throw new RuntimeException(
						"Failed to start timer for clockdrift config,", e);
			}
		}
	}

}
