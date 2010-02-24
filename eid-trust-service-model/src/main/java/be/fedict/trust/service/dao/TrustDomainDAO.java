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

package be.fedict.trust.service.dao;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.Local;

import be.fedict.trust.constraints.CertificatePoliciesCertificateConstraint;
import be.fedict.trust.constraints.KeyUsageCertificateConstraint;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.entity.constraints.DNConstraintEntity;
import be.fedict.trust.service.entity.constraints.EndEntityConstraintEntity;
import be.fedict.trust.service.entity.constraints.KeyUsageType;
import be.fedict.trust.service.entity.constraints.QCStatementsConstraintEntity;
import be.fedict.trust.service.exception.TrustDomainNotFoundException;

/**
 * Trust Domain DAO.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface TrustDomainDAO {

	/**
	 * Returns list of {@link TrustDomainEntity}'s
	 */
	List<TrustDomainEntity> listTrustDomains();

	/**
	 * Returns list of {@link TrustDomainEntity}'s containing the specified
	 * {@link TrustPointEntity}.
	 * 
	 * @param trustPoint
	 */
	List<TrustDomainEntity> listTrustDomains(TrustPointEntity trustPoint);

	/**
	 * Return {@link TrustDomainEntity} from specified name. Returns
	 * <code>null</code> if not found.
	 * 
	 * @param name
	 */
	TrustDomainEntity findTrustDomain(String name);

	/**
	 * Returns {@link TrustDomainEntity} from specified name. Throws
	 * {@link TrustDomainNotFoundException} if not found.
	 * 
	 * @param name
	 * 
	 * @throws TrustDomainNotFoundException
	 */
	TrustDomainEntity getTrustDomain(String name)
			throws TrustDomainNotFoundException;

	/**
	 * Create a new {@link TrustDomainEntity}.
	 * 
	 * @param name
	 * @param crlRefreshCron
	 */
	TrustDomainEntity addTrustDomain(String name, String crlRefreshCron);

	/**
	 * Sets the {@link TrustDomainEntity} as default.
	 * 
	 * @param trustDomain
	 */
	void setDefaultTrustDomain(TrustDomainEntity trustDomain);

	/**
	 * Returns the default {@link TrustDomainEntity}.
	 */
	TrustDomainEntity getDefaultTrustDomain();

	/**
	 * Create a new {@link TrustPointEntity}.
	 * 
	 * @param crlRefreshCron
	 * @param ca
	 */
	TrustPointEntity addTrustPoint(String crlRefreshCron,
			CertificateAuthorityEntity ca);

	/**
	 * Returns list of all {@link TrustPointEntity}'s.
	 */
	List<TrustPointEntity> listTrustPoints();

	/**
	 * Returns list of {@link TrustPointEntity}'s attached to the specified
	 * {@link TrustDomainEntity}.
	 * 
	 * @param trustDomain
	 */
	List<TrustPointEntity> listTrustPoints(TrustDomainEntity trustDomain);

	/**
	 * Returns list of {@link TrustPointEntity}'s attached to the specified
	 * {@link TrustDomainEntity} specified by trust domain name.
	 * 
	 * @param trustDomainName
	 * @throws TrustDomainNotFoundException
	 */
	List<TrustPointEntity> listTrustPoints(String trustDomainName)
			throws TrustDomainNotFoundException;

	/**
	 * Returns list of {@link CertificateAuthorityEntity}'s for the specified
	 * {@link TrustPointEntity}.
	 * 
	 * @param trustPoint
	 */
	List<CertificateAuthorityEntity> listCertificateAuthorities(
			TrustPointEntity trustPoint);

	/**
	 * Removes the selected {@link TrustPointEntity}.
	 * 
	 * @param trustPoint
	 */
	void removeTrustPoint(TrustPointEntity trustPoint);

	/**
	 * Returns the attached {@link TrustPointEntity}.
	 * 
	 * @param trustPoint
	 * @return
	 */
	TrustPointEntity attachTrustPoint(TrustPointEntity trustPoint);

	/**
	 * Add a new {@link CertificatePoliciesCertificateConstraint} to the
	 * {@link TrustDomainEntity}'s list of certificate contraints.
	 * 
	 * @param trustDomain
	 * @param policy
	 */
	void addCertificatePolicy(TrustDomainEntity trustDomain, String policy);

	/**
	 * Add a new {@link EndEntityConstraintEntity} to the
	 * {@link TrustDomainEntity}'s list of certificate contraints.
	 * 
	 * @param trustDomain
	 * @param certificate
	 */
	void addEndEntityConstraint(TrustDomainEntity trustDomain,
			X509Certificate certificate);

	/**
	 * Add a new {@link KeyUsageCertificateConstraint} to the
	 * {@link TrustDomainEntity}'s list of certificate contraints.
	 * 
	 * @param trustDomain
	 * @param keyUsageType
	 * @param allowed
	 */
	void addKeyUsageConstraint(TrustDomainEntity trustDomain,
			KeyUsageType keyUsageType, boolean allowed);

	/**
	 * Add a new {@link QCStatementsConstraintEntity} to the
	 * {@link TrustDomainEntity}'s list of certificate contraints.
	 * 
	 * @param trustDomain
	 * @param qcComplianceFilter
	 */
	void addQCStatementsConstraint(TrustDomainEntity trustDomain,
			boolean qcComplianceFilter);

	/**
	 * Add a new {@link DNConstraintEntity} to the {@link TrustDomainEntity}'s
	 * list of certificate contraints.
	 * 
	 * @param trustDomain
	 * @param dn
	 */
	void addDNConstraint(TrustDomainEntity trustDomain, String dn);

	/**
	 * Returns the {@link TrustPointEntity} from the specified name. Returns
	 * <code>null</code> if not found.
	 * 
	 * @param name
	 */
	TrustPointEntity findTrustPoint(String name);

}
