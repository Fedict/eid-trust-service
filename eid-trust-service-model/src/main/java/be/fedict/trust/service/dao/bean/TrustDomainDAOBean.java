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

package be.fedict.trust.service.dao.bean;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
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
import be.fedict.trust.service.exception.TrustDomainNotFoundException;
import be.fedict.trust.service.exception.VirtualTrustDomainNotFoundException;

/**
 * Trust Domain DAO Bean implementation.
 * 
 * @author wvdhaute
 */
@Stateless
public class TrustDomainDAOBean implements TrustDomainDAO {

	private static final Log LOG = LogFactory.getLog(TrustDomainDAOBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<TrustDomainEntity> listTrustDomains() {

		LOG.debug("list trust domains");
		Query query = this.entityManager
				.createNamedQuery(TrustDomainEntity.QUERY_LIST_ALL);
		return (List<TrustDomainEntity>) query.getResultList();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<VirtualTrustDomainEntity> listVirtualTrustDomains() {

		LOG.debug("list virtual trust domains");
		Query query = this.entityManager
				.createNamedQuery(VirtualTrustDomainEntity.QUERY_LIST_ALL);
		return (List<VirtualTrustDomainEntity>) query.getResultList();
	}

	/**
	 * {@inheritDoc}
	 */
	public TrustDomainEntity findTrustDomain(String name) {

		LOG.debug("find trust domain: " + name);
		return this.entityManager.find(TrustDomainEntity.class, name);
	}

	/**
	 * {@inheritDoc}
	 */
	public TrustDomainEntity getTrustDomain(String name)
			throws TrustDomainNotFoundException {
		LOG.debug("get trust domain: " + name);
		TrustDomainEntity trustDomain = findTrustDomain(name);
		if (null == trustDomain) {
			throw new TrustDomainNotFoundException();
		}
		return trustDomain;
	}

	/**
	 * {@inheritDoc}
	 */
	public VirtualTrustDomainEntity findVirtualTrustDomain(String name) {

		LOG.debug("find virtual trust domain: " + name);
		return this.entityManager.find(VirtualTrustDomainEntity.class, name);
	}

	/**
	 * {@inheritDoc}
	 */
	public VirtualTrustDomainEntity getVirtualTrustDomain(String name)
			throws VirtualTrustDomainNotFoundException {
		LOG.debug("get virtual trust domain: " + name);
		VirtualTrustDomainEntity virtualTrustDomain = findVirtualTrustDomain(name);
		if (null == virtualTrustDomain) {
			throw new VirtualTrustDomainNotFoundException();
		}
		return virtualTrustDomain;
	}

	/**
	 * {@inheritDoc}
	 */
	public TrustDomainEntity addTrustDomain(String name) {

		LOG.debug("add trust domain name=" + name);
		TrustDomainEntity trustDomain = new TrustDomainEntity(name);
		this.entityManager.persist(trustDomain);
		return trustDomain;
	}

	/**
	 * {@inheritDoc}
	 */
	public VirtualTrustDomainEntity addVirtualTrustDomain(String name) {

		LOG.debug("add virtual trust domain name=" + name);
		VirtualTrustDomainEntity virtualTrustDomain = new VirtualTrustDomainEntity(
				name);
		this.entityManager.persist(virtualTrustDomain);
		return virtualTrustDomain;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeTrustDomain(TrustDomainEntity trustDomain) {

		LOG.debug("remove trust domain " + trustDomain.getName());
		TrustDomainEntity attachedTrustDomain = findTrustDomain(trustDomain
				.getName());
		this.entityManager.remove(attachedTrustDomain);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeVirtualTrustDomain(
			VirtualTrustDomainEntity virtualTrustDomain) {

		LOG.debug("remove virtual trust domain " + virtualTrustDomain.getName());
		VirtualTrustDomainEntity attachedVirtualTrustDomain = findVirtualTrustDomain(virtualTrustDomain
				.getName());
		this.entityManager.remove(attachedVirtualTrustDomain);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDefaultTrustDomain(TrustDomainEntity trustDomain) {

		LOG.debug("set trust domain name=" + trustDomain.getName()
				+ " as default");
		for (TrustDomainEntity t : listTrustDomains()) {
			if (t.equals(trustDomain)) {
				t.setDefaultDomain(true);
			} else {
				t.setDefaultDomain(false);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public TrustDomainEntity getDefaultTrustDomain() {

		LOG.debug("return the default trust domain");
		Query query = this.entityManager
				.createNamedQuery(TrustDomainEntity.QUERY_GET_DEFAULT);
		return (TrustDomainEntity) query.getSingleResult();
	}

	/**
	 * {@inheritDoc}
	 */
	public TrustPointEntity addTrustPoint(String crlRefreshCronSchedule,
			CertificateAuthorityEntity ca) {

		LOG.debug("add trust point " + ca.getName()
				+ " crlRefreshCronSchedule=" + crlRefreshCronSchedule);
		TrustPointEntity trustPoint = new TrustPointEntity(
				crlRefreshCronSchedule, ca);
		this.entityManager.persist(trustPoint);
		return trustPoint;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<CertificateAuthorityEntity> listCertificateAuthorities(
			TrustPointEntity trustPoint) {

		LOG.debug("list CA's for trust point " + trustPoint.getName());
		Query query = this.entityManager
				.createNamedQuery(CertificateAuthorityEntity.QUERY_WHERE_TRUST_POINT);
		query.setParameter("trustPoint", trustPoint);
		return (List<CertificateAuthorityEntity>) query.getResultList();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<String> listCANames(TrustPointEntity trustPoint) {

		LOG.debug("list CA names for trust point " + trustPoint.getName());
		Query query = this.entityManager
				.createNamedQuery(CertificateAuthorityEntity.QUERY_NAME_WHERE_TRUST_POINT);
		query.setParameter("trustPoint", trustPoint);
		return (List<String>) query.getResultList();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<TrustPointEntity> listTrustPoints() {

		LOG.debug("list all trust points");
		Query query = this.entityManager
				.createNamedQuery(TrustPointEntity.QUERY_ALL);
		return (List<TrustPointEntity>) query.getResultList();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<TrustPointEntity> listTrustPoints(TrustDomainEntity trustDomain) {

		LOG.debug("list trust points for trust domain " + trustDomain.getName());
		Query query = this.entityManager
				.createNamedQuery(TrustDomainEntity.QUERY_LIST_TRUST_POINTS);
		query.setParameter("name", trustDomain.getName());
		return (List<TrustPointEntity>) query.getResultList();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<TrustPointEntity> listTrustPoints(String trustDomainName)
			throws TrustDomainNotFoundException {

		LOG.debug("list trust points for trust domain " + trustDomainName);
		TrustDomainEntity trustDomain = getTrustDomain(trustDomainName);
		return listTrustPoints(trustDomain);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<TrustDomainEntity> listTrustDomains(TrustPointEntity trustPoint) {

		LOG.debug("list trust domains containing trust point "
				+ trustPoint.getName());
		Query query = this.entityManager
				.createNamedQuery(TrustDomainEntity.QUERY_LIST_WHERE_TRUST_POINT);
		query.setParameter("trustPoint", trustPoint);
		return (List<TrustDomainEntity>) query.getResultList();
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeTrustPoint(TrustPointEntity trustPoint) {

		LOG.debug("remove trust point " + trustPoint.getName());
		TrustPointEntity attachedTrustPoint = attachTrustPoint(trustPoint);
		LOG.debug("remove attached trust point " + attachedTrustPoint.getName());
		this.entityManager.remove(attachedTrustPoint);
	}

	/**
	 * {@inheritDoc}
	 */
	public TrustPointEntity attachTrustPoint(TrustPointEntity trustPoint) {

		return this.entityManager.find(TrustPointEntity.class,
				trustPoint.getName());
	}

	/**
	 * {@inheritDoc}
	 */
	public PolicyConstraintEntity addCertificatePolicy(
			TrustDomainEntity trustDomain, String policy) {

		PolicyConstraintEntity certificatePolicy = new PolicyConstraintEntity(
				trustDomain, policy);
		this.entityManager.persist(certificatePolicy);
		TrustDomainEntity attachedTrustDomain = findTrustDomain(trustDomain
				.getName());
		attachedTrustDomain.getCertificateConstraints().add(certificatePolicy);
		return certificatePolicy;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeCertificateConstraint(
			CertificateConstraintEntity certificateConstraint) {
		CertificateConstraintEntity attachedCertificateConstraint = this.entityManager
				.find(CertificateConstraintEntity.class,
						certificateConstraint.getId());
		attachedCertificateConstraint.getTrustDomain()
				.getCertificateConstraints().remove(certificateConstraint);
		attachedCertificateConstraint.setTrustDomain(null);
		this.entityManager.flush();
		this.entityManager.remove(attachedCertificateConstraint);
	}

	/**
	 * {@inheritDoc}
	 */
	public EndEntityConstraintEntity addEndEntityConstraint(
			TrustDomainEntity trustDomain, X509Certificate certificate) {

		String issuerName = certificate.getIssuerX500Principal().getName();
		BigInteger serialNumber = certificate.getSerialNumber();
		EndEntityConstraintEntity endEntityCertificateConstraint = new EndEntityConstraintEntity(
				trustDomain, issuerName, serialNumber);
		this.entityManager.persist(endEntityCertificateConstraint);
		TrustDomainEntity attachedTrustDomain = findTrustDomain(trustDomain
				.getName());
		attachedTrustDomain.getCertificateConstraints().add(
				endEntityCertificateConstraint);
		return endEntityCertificateConstraint;
	}

	/**
	 * {@inheritDoc}
	 */
	public KeyUsageConstraintEntity addKeyUsageConstraint(
			TrustDomainEntity trustDomain, KeyUsageType keyUsageType,
			boolean allowed) {

		KeyUsageConstraintEntity keyUsageConstraint = new KeyUsageConstraintEntity(
				trustDomain, keyUsageType, allowed);
		this.entityManager.persist(keyUsageConstraint);
		TrustDomainEntity attachedTrustDomain = findTrustDomain(trustDomain
				.getName());
		attachedTrustDomain.getCertificateConstraints().add(keyUsageConstraint);
		return keyUsageConstraint;
	}

	/**
	 * {@inheritDoc}
	 */
	public QCStatementsConstraintEntity addQCStatementsConstraint(
			TrustDomainEntity trustDomain, boolean qcComplianceFilter) {

		QCStatementsConstraintEntity qcStatementsConstraint = new QCStatementsConstraintEntity(
				trustDomain, qcComplianceFilter);
		this.entityManager.persist(qcStatementsConstraint);
		TrustDomainEntity attachedTrustDomain = findTrustDomain(trustDomain
				.getName());
		attachedTrustDomain.getCertificateConstraints().add(
				qcStatementsConstraint);
		return qcStatementsConstraint;
	}

	/**
	 * {@inheritDoc}
	 */
	public DNConstraintEntity addDNConstraint(TrustDomainEntity trustDomain,
			String dn) {

		DNConstraintEntity dnConstraint = new DNConstraintEntity(trustDomain,
				dn);
		this.entityManager.persist(dnConstraint);
		TrustDomainEntity attachedTrustDomain = findTrustDomain(trustDomain
				.getName());
		attachedTrustDomain.getCertificateConstraints().add(dnConstraint);
		return dnConstraint;
	}

	/**
	 * {@inheritDoc}
	 */
	public TSAConstraintEntity addTSAConstraint(TrustDomainEntity trustDomain) {

		TSAConstraintEntity tsaConstraint = new TSAConstraintEntity(trustDomain);
		this.entityManager.persist(tsaConstraint);
		TrustDomainEntity attachedTrustDomain = findTrustDomain(trustDomain
				.getName());
		attachedTrustDomain.getCertificateConstraints().add(tsaConstraint);
		return tsaConstraint;
	}

	/**
	 * {@inheritDoc}
	 */
	public TrustPointEntity findTrustPoint(String name) {

		LOG.debug("find trust point: " + name);
		return this.entityManager.find(TrustPointEntity.class, name);
	}

	/**
	 * {@inheritDoc}
	 */
	public CertificateConstraintEntity findCertificateConstraint(
			CertificateConstraintEntity certificateConstraint) {

		return this.entityManager.find(CertificateConstraintEntity.class,
				certificateConstraint.getId());
	}
}
