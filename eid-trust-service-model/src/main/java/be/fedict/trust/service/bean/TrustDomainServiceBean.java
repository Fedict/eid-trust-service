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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.NotificationService;
import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.TrustDomainService;
import be.fedict.trust.service.dao.CertificateAuthorityDAO;
import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.Status;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.entity.VirtualTrustDomainEntity;
import be.fedict.trust.service.entity.constraints.CertificateConstraintEntity;
import be.fedict.trust.service.entity.constraints.DNConstraintEntity;
import be.fedict.trust.service.entity.constraints.EndEntityConstraintEntity;
import be.fedict.trust.service.entity.constraints.KeyUsageConstraintEntity;
import be.fedict.trust.service.entity.constraints.KeyUsageType;
import be.fedict.trust.service.entity.constraints.PolicyConstraintEntity;
import be.fedict.trust.service.entity.constraints.QCStatementsConstraintEntity;
import be.fedict.trust.service.entity.constraints.TSAConstraintEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.exception.TrustDomainAlreadyExistsException;
import be.fedict.trust.service.exception.TrustDomainNotFoundException;
import be.fedict.trust.service.exception.TrustPointAlreadyExistsException;
import be.fedict.trust.service.exception.VirtualTrustDomainAlreadyExistsException;
import be.fedict.trust.service.exception.VirtualTrustDomainNotFoundException;

/**
 * Trust Domain Service Bean implementation.
 * 
 * @author wvdhaute
 */
@Stateless
public class TrustDomainServiceBean implements TrustDomainService {

	private static final Log LOG = LogFactory
			.getLog(TrustDomainServiceBean.class);

	@EJB
	private TrustDomainDAO trustDomainDAO;

	@EJB
	private CertificateAuthorityDAO certificateAuthorityDAO;

	@EJB
	private SchedulingService schedulingService;

	@EJB
	private NotificationService notificationService;

	/**
	 * {@inheritDoc}
	 */
	public TrustDomainEntity addTrustDomain(String name)
			throws TrustDomainAlreadyExistsException {

		if (null != this.trustDomainDAO.findTrustDomain(name)) {
			LOG.error("Trust domain: " + name + " already exists");
			throw new TrustDomainAlreadyExistsException();
		}
		LOG.debug("add trust domain: " + name);
		return this.trustDomainDAO.addTrustDomain(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public VirtualTrustDomainEntity addVirtualTrustDomain(String name)
			throws VirtualTrustDomainAlreadyExistsException,
			TrustDomainAlreadyExistsException {

		if (null != this.trustDomainDAO.findVirtualTrustDomain(name)) {
			LOG.error("Virtual Trust domain: " + name + " already exists");
			throw new VirtualTrustDomainAlreadyExistsException();
		}
		if (null != this.trustDomainDAO.findTrustDomain(name)) {
			LOG.error("Trust domain: " + name + " already exists");
			throw new TrustDomainAlreadyExistsException();
		}
		LOG.debug("add virtualtrust domain: " + name);
		return this.trustDomainDAO.addVirtualTrustDomain(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeTrustDomain(TrustDomainEntity trustDomain) {

		LOG.debug("remove trust domain: " + trustDomain.getName());
		this.trustDomainDAO.removeTrustDomain(trustDomain);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeVirtualTrustDomain(
			VirtualTrustDomainEntity virtualTrustDomain) {

		LOG.debug("remove virtual trust domain: "
				+ virtualTrustDomain.getName());
		this.trustDomainDAO.removeVirtualTrustDomain(virtualTrustDomain);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<TrustDomainEntity> listTrustDomains() {

		LOG.debug("list trust domains");
		return this.trustDomainDAO.listTrustDomains();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<VirtualTrustDomainEntity> listVirtualTrustDomains() {

		LOG.debug("list virtual trust domains");
		return this.trustDomainDAO.listVirtualTrustDomains();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<TrustPointEntity> listTrustPoints() {

		LOG.debug("list trust points");
		return this.trustDomainDAO.listTrustPoints();
	}

	/**
	 * {@inheritDoc}
	 */
	public void save(TrustDomainEntity trustDomain) {

		LOG.debug("save trust domain: " + trustDomain.getName());
		TrustDomainEntity attachedTrustDomain = this.trustDomainDAO
				.findTrustDomain(trustDomain.getName());
		attachedTrustDomain.setUseCaching(trustDomain.isUseCaching());
	}

	/**
	 * {@inheritDoc}
	 */
	public void save(TrustPointEntity trustPoint)
			throws InvalidCronExpressionException {

		LOG.debug("save trust point: " + trustPoint.getName());
		TrustPointEntity attachedTrustPoint = this.trustDomainDAO
				.attachTrustPoint(trustPoint);
		attachedTrustPoint.setCrlRefreshCronSchedule(trustPoint
				.getCrlRefreshCronSchedule());
		this.schedulingService.startTimer(attachedTrustPoint);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<TrustPointEntity> listTrustPoints(TrustDomainEntity trustDomain) {

		LOG.debug("list trust points for " + trustDomain.getName());
		return this.trustDomainDAO.listTrustPoints(trustDomain);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDefault(TrustDomainEntity trustDomain) {

		LOG.debug("set default trust domain: " + trustDomain.getName());
		this.trustDomainDAO.setDefaultTrustDomain(trustDomain);

	}

	/**
	 * {@inheritDoc}
	 */
	public void removeTrustPoint(TrustPointEntity trustPoint) {

		LOG.debug("remove trust point: " + trustPoint.getName());
		TrustPointEntity attachedTrustPoint = this.trustDomainDAO
				.attachTrustPoint(trustPoint);
		attachedTrustPoint.getCertificateAuthority().setTrustPoint(null);

		// remove timers
		this.schedulingService.cancelTimers(attachedTrustPoint.getName());

		// remove cache for each CA
		for (String caName : this.trustDomainDAO.listCANames(trustPoint)) {
			this.certificateAuthorityDAO.removeRevokedCertificates(caName);
		}

		// remove CA's
		this.certificateAuthorityDAO
				.removeCertificateAuthorities(attachedTrustPoint);

		// remove trust point from all trustdomains
		List<TrustDomainEntity> trustDomains = this.trustDomainDAO
				.listTrustDomains(attachedTrustPoint);
		for (TrustDomainEntity trustDomain : trustDomains) {
			LOG.debug("remove trust point " + attachedTrustPoint.getName()
					+ " from trust domain " + trustDomain.getName());
			trustDomain.getTrustPoints().remove(attachedTrustPoint);
		}

		// remove trust point
		this.trustDomainDAO.removeTrustPoint(attachedTrustPoint);
	}

	/**
	 * {@inheritDoc}
	 */
	public TrustPointEntity addTrustPoint(String crlRefreshCronSchedule,
			byte[] certificateBytes) throws TrustPointAlreadyExistsException,
			CertificateException, InvalidCronExpressionException {

		LOG.debug("add trust point");

		X509Certificate certificate = getCertificate(certificateBytes);

		// add CA
		if (null != this.certificateAuthorityDAO
				.findCertificateAuthority(certificate)) {
			LOG.error("trust point already exist: "
					+ certificate.getSubjectX500Principal().toString());
			throw new TrustPointAlreadyExistsException();
		}
		CertificateAuthorityEntity certificateAuthority = this.certificateAuthorityDAO
				.addCertificateAuthority(certificate, null);

		// add trust point
		TrustPointEntity trustPoint = this.trustDomainDAO.addTrustPoint(
				crlRefreshCronSchedule, certificateAuthority);

		// manage relationship
		certificateAuthority.setTrustPoint(trustPoint);

		// start timer
		this.schedulingService.startTimer(trustPoint);

		return trustPoint;
	}

	private X509Certificate getCertificate(byte[] certificateBytes)
			throws CertificateException {

		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		return (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(certificateBytes));
	}

	/**
	 * {@inheritDoc}
	 */
	public TrustPointEntity findTrustPoint(String name) {

		return this.trustDomainDAO.findTrustPoint(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTrustPoints(TrustDomainEntity trustDomain,
			List<String> trustPointNames) throws TrustDomainNotFoundException {

		LOG.debug("set selected trust points for domain: "
				+ trustDomain.getName());
		TrustDomainEntity attachedTrustDomain = this.trustDomainDAO
				.getTrustDomain(trustDomain.getName());
		List<TrustPointEntity> trustPoints = new LinkedList<TrustPointEntity>();
		for (String trustPointName : trustPointNames) {
			trustPoints.add(this.trustDomainDAO.findTrustPoint(trustPointName));
		}
		attachedTrustDomain.setTrustPoints(trustPoints);
	}

	/**
	 * {@inheritDoc}
	 */
	public VirtualTrustDomainEntity setTrustDomains(
			VirtualTrustDomainEntity virtualTrustDomain,
			List<String> trustDomainNames)
			throws VirtualTrustDomainNotFoundException {

		LOG.debug("set selected trust domains for virtual domain: "
				+ virtualTrustDomain.getName());
		VirtualTrustDomainEntity attachedVirtualTrustDomain = this.trustDomainDAO
				.getVirtualTrustDomain(virtualTrustDomain.getName());
		Set<TrustDomainEntity> trustDomains = new HashSet<TrustDomainEntity>();
		for (String trustDomainName : trustDomainNames) {
			trustDomains.add(this.trustDomainDAO
					.findTrustDomain(trustDomainName));
		}
		attachedVirtualTrustDomain.setTrustDomains(trustDomains);
		return attachedVirtualTrustDomain;
	}

	/**
	 * {@inheritDoc}
	 */
	public PolicyConstraintEntity addCertificatePolicy(
			TrustDomainEntity trustDomain, String policy) {

		LOG.debug("add certificate policy \"" + policy + "\" to trust domain "
				+ trustDomain.getName());
		return this.trustDomainDAO.addCertificatePolicy(trustDomain, policy);
	}

	/**
	 * {@inheritDoc}
	 */
	public KeyUsageConstraintEntity addKeyUsageConstraint(
			TrustDomainEntity trustDomain, KeyUsageType keyUsage,
			boolean allowed) {

		LOG.debug("add key usage constraint " + keyUsage + " allowed="
				+ allowed);
		return this.trustDomainDAO.addKeyUsageConstraint(trustDomain, keyUsage,
				allowed);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveKeyUsageConstraints(
			List<KeyUsageConstraintEntity> keyUsageConstraints) {

		LOG.debug("save key usage constraints");
		for (KeyUsageConstraintEntity keyUsageConstraint : keyUsageConstraints) {
			KeyUsageConstraintEntity attachedKeyUsageConstraint = (KeyUsageConstraintEntity) this.trustDomainDAO
					.findCertificateConstraint(keyUsageConstraint);
			attachedKeyUsageConstraint.setAllowed(keyUsageConstraint
					.isAllowed());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public DNConstraintEntity addDNConstraint(TrustDomainEntity trustDomain,
			String dn) {

		LOG.debug("Add DN constraint: " + dn);
		return this.trustDomainDAO.addDNConstraint(trustDomain, dn);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveDNConstraint(DNConstraintEntity dnConstraint) {

		LOG.debug("Save DN constraint: " + dnConstraint);
		DNConstraintEntity attachedDNConstraint = (DNConstraintEntity) this.trustDomainDAO
				.findCertificateConstraint(dnConstraint);
		attachedDNConstraint.setDn(dnConstraint.getDn());
	}

	/**
	 * {@inheritDoc}
	 */
	public EndEntityConstraintEntity addEndEntityConstraint(
			TrustDomainEntity trustDomain, byte[] certificateBytes)
			throws CertificateException {

		LOG.debug("add end entity constraint");
		return this.trustDomainDAO.addEndEntityConstraint(trustDomain,
				getCertificate(certificateBytes));
	}

	/**
	 * {@inheritDoc}
	 */
	public QCStatementsConstraintEntity addQCConstraint(
			TrustDomainEntity trustDomain, boolean qc) {

		LOG.debug("Add QC constraint: " + qc);
		return this.trustDomainDAO.addQCStatementsConstraint(trustDomain, qc);
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveQCConstraint(QCStatementsConstraintEntity qcConstraint) {

		LOG.debug("Save QC constraint: " + qcConstraint);
		QCStatementsConstraintEntity attachedQcStatementsConstraint = (QCStatementsConstraintEntity) this.trustDomainDAO
				.findCertificateConstraint(qcConstraint);
		attachedQcStatementsConstraint.setQcComplianceFilter(qcConstraint
				.getQcComplianceFilter());
	}

	/**
	 * {@inheritDoc}
	 */
	public TSAConstraintEntity addTSAConstraint(TrustDomainEntity trustDomain) {

		LOG.debug("Add TSA constraint");
		return this.trustDomainDAO.addTSAConstraint(trustDomain);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeCertificateConstraint(
			CertificateConstraintEntity certificateConstraint) {

		LOG.debug("Remove certificate constraint: "
				+ certificateConstraint.getClass());
		this.trustDomainDAO.removeCertificateConstraint(certificateConstraint);

	}

	/**
	 * {@inheritDoc}
	 */
	public void refreshTrustPointCache(TrustPointEntity trustPoint) {

		LOG.debug("refresh trust point revocation cache: "
				+ trustPoint.getName());
		this.schedulingService.startTimerNow(trustPoint);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<CertificateAuthorityEntity> listTrustPointCAs(
			TrustPointEntity trustPoint) {

		LOG.debug("list CA's for trust point: " + trustPoint.getName());
		return this.trustDomainDAO.listCertificateAuthorities(trustPoint);
	}

	/**
	 * {@inheritDoc}
	 */
	public void refreshCACache(CertificateAuthorityEntity ca)
			throws JMSException {

		LOG.debug("refresh CA revocation cache: " + ca.getName());
		this.schedulingService.refreshCA(ca);
	}

	public List<CertificateAuthorityEntity> listCAs() {
		return this.certificateAuthorityDAO.listCertificateAuthorities();
	}

	public long getTotalCachedCertificates() {
		return this.certificateAuthorityDAO.getTotalCachedCertificates();
	}

	public long getTotalActiveCachedCAs() {
		return this.certificateAuthorityDAO.getTotalActiveCachedCAs();
	}

	public void coldStart(String crlUrl, String certUrl) {
		LOG.debug("cold start: " + crlUrl + "; " + certUrl);
		if (null == crlUrl) {
			return;
		}
		if (null == certUrl) {
			return;
		}
		if (crlUrl.isEmpty()) {
			return;
		}
		if (certUrl.isEmpty()) {
			return;
		}
		try {
			this.notificationService.notifyColdStart(crlUrl, certUrl);
		} catch (JMSException e) {
			LOG.error("JMS error: " + e.getMessage(), e);
		}
	}

	public void coldStart(String crlText) {
		LOG.debug("coldstart from text file: " + crlText);
		if (null == crlText) {
			return;
		}
		if (crlText.isEmpty()) {
			return;
		}

		BufferedReader bufferedReader = new BufferedReader(new StringReader(
				crlText));

		String crlUrl;
		try {
			while (null != (crlUrl = bufferedReader.readLine())) {
				crlUrl = crlUrl.trim();
				String certUrl = bufferedReader.readLine();
				certUrl = certUrl.trim();
				try {
					this.notificationService.notifyColdStart(crlUrl, certUrl);
				} catch (JMSException e) {
					LOG.error("JMS error: " + e.getMessage(), e);
				}
			}
		} catch (IOException e) {
			LOG.error("error parsing CRL text file: " + e.getMessage(), e);
		}
	}

	public void removeCACache(CertificateAuthorityEntity ca)
			throws JMSException {
		String caName = ca.getName();
		LOG.debug("remove CA cache: " + caName);
		/*
		 * We first disable the cache for this CA.
		 */
		CertificateAuthorityEntity caEntity = this.certificateAuthorityDAO
				.findCertificateAuthority(caName);
		if (null == caEntity) {
			LOG.error("CA entity not found: " + caName);
			return;
		}
		caEntity.setStatus(Status.NONE);
		/*
		 * Next we notify the scheduling service that the CRL cache for this CA
		 * can be cleared.
		 */
		this.notificationService.notifyRemoveCA(caName);
	}
}
