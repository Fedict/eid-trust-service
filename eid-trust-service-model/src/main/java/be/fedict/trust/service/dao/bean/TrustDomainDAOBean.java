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

package be.fedict.trust.service.dao.bean;

import java.security.cert.CertificateEncodingException;
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
import be.fedict.trust.service.exception.TrustDomainNotFoundException;

/**
 * Trust Domain DAO Bean implementation.
 * 
 * @author wvdhaute
 * 
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
		if (null == trustDomain)
			throw new TrustDomainNotFoundException();
		return trustDomain;
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
	public TrustDomainEntity addTrustDomain(String name, String crlRefreshCron) {

		LOG.debug("add trust domain name=" + name + " crlRefreshCron="
				+ crlRefreshCron);
		TrustDomainEntity trustDomain = new TrustDomainEntity(name,
				crlRefreshCron);
		this.entityManager.persist(trustDomain);
		return trustDomain;
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
	public CertificateAuthorityEntity addCertificateAuthority(String crlUrl,
			X509Certificate certificate, TrustPointEntity trustPoint) {

		LOG
				.debug("add CA: "
						+ certificate.getSubjectX500Principal().toString());
		CertificateAuthorityEntity certificateAuthority;
		try {
			certificateAuthority = new CertificateAuthorityEntity(certificate
					.getSubjectX500Principal().toString(), crlUrl, certificate,
					trustPoint);
		} catch (CertificateEncodingException e) {
			LOG.error("Certificate encoding exception: " + e.getMessage());
			return null;
		}
		this.entityManager.persist(certificateAuthority);
		return certificateAuthority;
	}

	/**
	 * {@inheritDoc}
	 */
	public TrustPointEntity addTrustPoint(String name, String crlRefreshCron,
			TrustDomainEntity trustDomain, CertificateAuthorityEntity ca) {

		LOG.debug("add trust point name=" + name + " crlRefreshCron="
				+ crlRefreshCron);
		TrustPointEntity trustPoint = new TrustPointEntity(name,
				crlRefreshCron, trustDomain, ca);
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
	public void removeCertificateAuthorities(TrustPointEntity trustPoint) {

		LOG.debug("remove CA's for trust point " + trustPoint.getName());
		Query query = this.entityManager
				.createNamedQuery(CertificateAuthorityEntity.DELETE_WHERE_TRUST_POINT);
		query.setParameter("trustPoint", trustPoint);
		int result = query.executeUpdate();
		LOG.debug("CA's removed: " + result);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public List<TrustPointEntity> listTrustPoints(TrustDomainEntity trustDomain) {

		LOG
				.debug("list trust points for trust domain "
						+ trustDomain.getName());
		Query query = this.entityManager
				.createNamedQuery(TrustPointEntity.QUERY_WHERE_TRUST_DOMAIN);
		query.setParameter("trustDomain", trustDomain);
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
	public void removeTrustPoint(TrustPointEntity trustPoint) {

		LOG.debug("remove trust point " + trustPoint.getName());
		TrustPointEntity attachedTrustPoint = this.entityManager.find(
				TrustPointEntity.class, trustPoint.getName());
		this.entityManager.remove(attachedTrustPoint);
	}
}
