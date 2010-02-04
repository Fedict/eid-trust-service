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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.SecurityDomain;

import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.TimerInfo;
import be.fedict.trust.service.TrustDomainService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.exception.CertificateAuthorityAlreadyExistsException;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.exception.TrustPointAlreadyExistsException;

/**
 * Trust Domain Service Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
@SecurityDomain(TrustServiceConstants.ADMIN_SECURITY_DOMAIN)
public class TrustDomainServiceBean implements TrustDomainService {

	private static final Log LOG = LogFactory
			.getLog(TrustDomainServiceBean.class);

	@EJB
	private TrustDomainDAO trustDomainDAO;

	@EJB
	private SchedulingService schedulingService;

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public List<TrustDomainEntity> listTrustDomains() {

		LOG.debug("list trust domains");
		return this.trustDomainDAO.listTrustDomains();
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public void save(TrustDomainEntity trustDomain)
			throws InvalidCronExpressionException {

		LOG.debug("save trust domain: " + trustDomain.getName());
		TrustDomainEntity attachedTrustDomain = this.trustDomainDAO
				.findTrustDomain(trustDomain.getName());
		attachedTrustDomain.setCrlRefreshCron(trustDomain.getCrlRefreshCron());
		this.schedulingService.startTimer(attachedTrustDomain, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public void save(TrustPointEntity trustPoint)
			throws InvalidCronExpressionException {

		LOG.debug("save trust point: " + trustPoint.getName());
		TrustPointEntity attachedTrustPoint = this.trustDomainDAO
				.findTrustPoint(trustPoint.getName());
		attachedTrustPoint.setCrlRefreshCron(trustPoint.getCrlRefreshCron());
		if (null != attachedTrustPoint.getCrlRefreshCron()
				&& !attachedTrustPoint.getCrlRefreshCron().equals("")) {
			this.schedulingService.startTimer(attachedTrustPoint, false);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public List<TrustPointEntity> listTrustPoints(TrustDomainEntity trustDomain) {

		LOG.debug("list trust points for " + trustDomain.getName());
		return this.trustDomainDAO.listTrustPoints(trustDomain);
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public void setDefault(TrustDomainEntity trustDomain) {

		LOG.debug("set default trust domain: " + trustDomain.getName());
		this.trustDomainDAO.setDefaultTrustDomain(trustDomain);

	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public void remove(TrustPointEntity trustPoint) {

		LOG.debug("remove trust point: " + trustPoint.getName());

		// remove timers
		this.schedulingService.cancelTimers(new TimerInfo(trustPoint));

		// remove CA's
		this.trustDomainDAO.removeCertificateAuthorities(trustPoint);

		// remove trust point
		this.trustDomainDAO.removeTrustPoint(trustPoint);
	}

	/**
	 * {@inheritDoc}
	 */
	@RolesAllowed(TrustServiceConstants.ADMIN_ROLE)
	public TrustPointEntity addTrustPoint(String name, String crlRefreshCron,
			TrustDomainEntity trustDomain, byte[] certificateBytes)
			throws TrustPointAlreadyExistsException, CertificateException,
			InvalidCronExpressionException,
			CertificateAuthorityAlreadyExistsException {

		LOG.debug("add trust point: " + name);

		X509Certificate certificate = getCertificate(certificateBytes);

		// check already exists
		if (null != this.trustDomainDAO.findTrustPoint(name)) {
			LOG.error("trust point already exist: " + name);
			throw new TrustPointAlreadyExistsException();
		} else if (null != this.trustDomainDAO
				.findCertificateAuthority(certificate)) {
			LOG.error("CA already exist: "
					+ certificate.getSubjectX500Principal().toString());
			throw new CertificateAuthorityAlreadyExistsException();
		}

		// add CA
		CertificateAuthorityEntity certificateAuthority = this.trustDomainDAO
				.addCertificateAuthority(certificate, null);

		// add trust point
		TrustPointEntity trustPoint = this.trustDomainDAO.addTrustPoint(name,
				crlRefreshCron, trustDomain, certificateAuthority);

		// manage relationship
		certificateAuthority.setTrustPoint(trustPoint);

		// start timer if needed
		if (null != crlRefreshCron && !crlRefreshCron.isEmpty()) {
			this.schedulingService.startTimer(trustPoint, false);
		}

		return trustPoint;
	}

	private X509Certificate getCertificate(byte[] certificateBytes)
			throws CertificateException {

		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		X509Certificate certificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(certificateBytes));
		return certificate;
	}
}
