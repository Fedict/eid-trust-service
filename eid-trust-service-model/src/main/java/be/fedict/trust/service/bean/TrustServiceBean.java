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

import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.FallbackTrustLinker;
import be.fedict.trust.MemoryCertificateRepository;
import be.fedict.trust.NetworkConfig;
import be.fedict.trust.PublicKeyTrustLinker;
import be.fedict.trust.TrustLinker;
import be.fedict.trust.TrustValidator;
import be.fedict.trust.constraints.CertificatePoliciesCertificateConstraint;
import be.fedict.trust.constraints.DistinguishedNameCertificateConstraint;
import be.fedict.trust.constraints.EndEntityCertificateConstraint;
import be.fedict.trust.constraints.KeyUsageCertificateConstraint;
import be.fedict.trust.constraints.QCStatementsCertificateConstraint;
import be.fedict.trust.crl.CachedCrlRepository;
import be.fedict.trust.crl.CrlTrustLinker;
import be.fedict.trust.crl.OnlineCrlRepository;
import be.fedict.trust.ocsp.OcspTrustLinker;
import be.fedict.trust.ocsp.OnlineOcspRepository;
import be.fedict.trust.service.TrustService;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.entity.constraints.CertificateConstraintEntity;
import be.fedict.trust.service.entity.constraints.DNConstraintEntity;
import be.fedict.trust.service.entity.constraints.EndEntityConstraintEntity;
import be.fedict.trust.service.entity.constraints.KeyUsageConstraintEntity;
import be.fedict.trust.service.entity.constraints.PolicyConstraintEntity;
import be.fedict.trust.service.entity.constraints.QCStatementsConstraintEntity;

/**
 * Trust Service Bean implementation.
 * 
 * @author fcorneli
 * 
 */
@Stateless
public class TrustServiceBean implements TrustService {

	private static final Log LOG = LogFactory.getLog(TrustServiceBean.class);

	@EJB
	private ConfigurationDAO configurationDAO;

	@PersistenceContext
	private EntityManager entityManager;

	@Resource(mappedName = "java:JmsXA")
	private QueueConnectionFactory queueConnectionFactory;

	@Resource(mappedName = HarvesterMDB.HARVESTER_QUEUE_NAME)
	private Queue queue;

	@EJB
	private TrustDomainDAO trustDomainDAO;

	private TrustValidator getTrustValidator() {

		TrustLinker trustLinker = new TrustServiceTrustLinker(
				this.entityManager, this.queueConnectionFactory, this.queue);

		// XXX: for now just get the default domain
		TrustDomainEntity trustDomain = this.trustDomainDAO
				.getDefaultTrustDomain();

		return getTrustValidator(trustDomain, trustLinker);
	}

	private TrustValidator getTrustValidator(TrustDomainEntity trustDomain,
			TrustLinker trustLinker) {

		NetworkConfig networkConfig = configurationDAO.getNetworkConfig();

		MemoryCertificateRepository certificateRepository = new MemoryCertificateRepository();
		for (TrustPointEntity trustPoint : trustDomain.getTrustPoints()) {
			certificateRepository.addTrustPoint(trustPoint
					.getCertificateAuthority().getCertificate());
		}

		TrustValidator trustValidator = new TrustValidator(
				certificateRepository);
		trustValidator.addTrustLinker(new PublicKeyTrustLinker());

		OnlineOcspRepository ocspRepository = new OnlineOcspRepository(
				networkConfig);

		OnlineCrlRepository crlRepository = new OnlineCrlRepository(
				networkConfig);
		CachedCrlRepository cachedCrlRepository = new CachedCrlRepository(
				crlRepository);

		FallbackTrustLinker fallbackTrustLinker = new FallbackTrustLinker();
		if (null != trustLinker) {
			fallbackTrustLinker.addTrustLinker(trustLinker);
		}
		fallbackTrustLinker.addTrustLinker(new OcspTrustLinker(ocspRepository));
		fallbackTrustLinker.addTrustLinker(new CrlTrustLinker(
				cachedCrlRepository));

		trustValidator.addTrustLinker(fallbackTrustLinker);

		// add certificate constraints
		CertificatePoliciesCertificateConstraint certificatePoliciesCertificateConstraint = null;
		KeyUsageCertificateConstraint keyUsageCertificateConstraint = null;
		EndEntityCertificateConstraint endEntityCertificateConstraint = null;
		for (CertificateConstraintEntity certificateConstraint : trustDomain
				.getCertificateConstraints()) {

			if (certificateConstraint instanceof PolicyConstraintEntity) {

				PolicyConstraintEntity policyConstraint = (PolicyConstraintEntity) certificateConstraint;
				if (null == certificatePoliciesCertificateConstraint) {
					certificatePoliciesCertificateConstraint = new CertificatePoliciesCertificateConstraint();
				}
				certificatePoliciesCertificateConstraint
						.addCertificatePolicy(policyConstraint.getPolicy());

			} else if (certificateConstraint instanceof KeyUsageConstraintEntity) {

				KeyUsageConstraintEntity keyUsageConstraint = (KeyUsageConstraintEntity) certificateConstraint;
				if (null == keyUsageCertificateConstraint) {
					keyUsageCertificateConstraint = new KeyUsageCertificateConstraint();
				}
				switch (keyUsageConstraint.getKeyUsage()) {
				// XXX: complete after jtrust is updated for all KeyUsages
				case DIGITAL_SIGNATURE_IDX: {
					keyUsageCertificateConstraint
							.setDigitalSignatureFilter(keyUsageConstraint
									.getAllowed());
					break;
				}
				case NON_REPUDIATION_IDX: {
					keyUsageCertificateConstraint
							.setNonRepudiationFilter(keyUsageConstraint
									.getAllowed());
					break;
				}
				}

			} else if (certificateConstraint instanceof QCStatementsConstraintEntity) {

				QCStatementsConstraintEntity qcStatementsConstraint = (QCStatementsConstraintEntity) certificateConstraint;
				trustValidator
						.addCertificateConstrain(new QCStatementsCertificateConstraint(
								qcStatementsConstraint.getQcComplianceFilter()));

			} else if (certificateConstraint instanceof DNConstraintEntity) {

				DNConstraintEntity dnConstraint = (DNConstraintEntity) certificateConstraint;
				trustValidator
						.addCertificateConstrain(new DistinguishedNameCertificateConstraint(
								dnConstraint.getDn()));

			} else if (certificateConstraint instanceof EndEntityConstraintEntity) {

				EndEntityConstraintEntity endEntityConstraint = (EndEntityConstraintEntity) certificateConstraint;
				if (null == endEntityCertificateConstraint) {
					endEntityCertificateConstraint = new EndEntityCertificateConstraint();
				}
				endEntityCertificateConstraint
						.addEndEntity(endEntityConstraint.getIssuerName(),
								endEntityConstraint.getSerialNumber());
			}
		}

		if (null != certificatePoliciesCertificateConstraint) {
			trustValidator
					.addCertificateConstrain(certificatePoliciesCertificateConstraint);
		}
		if (null != keyUsageCertificateConstraint) {
			trustValidator
					.addCertificateConstrain(keyUsageCertificateConstraint);
		}
		if (null != endEntityCertificateConstraint) {
			trustValidator
					.addCertificateConstrain(endEntityCertificateConstraint);
		}

		return trustValidator;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean isValid(List<X509Certificate> authenticationCertificateChain) {
		LOG.debug("isValid: "
				+ authenticationCertificateChain.get(0)
						.getSubjectX500Principal());

		try {
			getTrustValidator().isTrusted(authenticationCertificateChain);
		} catch (CertPathValidatorException e) {
			LOG.debug("certificate path validation error: " + e.getMessage());
			return false;
		}
		return true;
	}
}
