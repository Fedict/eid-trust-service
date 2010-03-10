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

package be.fedict.trust.service;

import java.security.cert.CertificateException;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Timer;

import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.entity.constraints.DNConstraintEntity;
import be.fedict.trust.service.entity.constraints.EndEntityConstraintEntity;
import be.fedict.trust.service.entity.constraints.KeyUsageConstraintEntity;
import be.fedict.trust.service.entity.constraints.KeyUsageType;
import be.fedict.trust.service.entity.constraints.PolicyConstraintEntity;
import be.fedict.trust.service.entity.constraints.QCStatementsConstraintEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.exception.TrustDomainNotFoundException;
import be.fedict.trust.service.exception.TrustPointAlreadyExistsException;

/**
 * Trust domain service.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface TrustDomainService {

	/**
	 * List all {@link TrustDomainEntity}'s.
	 */
	List<TrustDomainEntity> listTrustDomains();

	/**
	 * List all {@link TrustPointEntity}'s.
	 */
	List<TrustPointEntity> listTrustPoints();

	/**
	 * List all {@link TrustPointEntity}'s for the specified
	 * {@link TrustDomainEntity}.
	 * 
	 * @param trustDomain
	 */
	List<TrustPointEntity> listTrustPoints(TrustDomainEntity trustDomain);

	/**
	 * Save the changes to the specified {@link TrustDomainEntity}
	 * 
	 * @param trustDomain
	 * 
	 * @throws InvalidCronExpressionException
	 */
	void save(TrustDomainEntity trustDomain)
			throws InvalidCronExpressionException;

	/**
	 * Save the changes to the specified {@link TrustPointEntity}
	 * 
	 * @param trustPoint
	 * 
	 * @throws InvalidCronExpressionException
	 */
	void save(TrustPointEntity trustPoint)
			throws InvalidCronExpressionException;

	/**
	 * Add a new {@link TrustPointEntity}.
	 * 
	 * @param crlRefreshCron
	 * @param certificateBytes
	 * 
	 * @throws TrustPointAlreadyExistsException
	 * @throws CertificateException
	 * @throws InvalidCronExpressionException
	 */
	TrustPointEntity addTrustPoint(String crlRefreshCron,
			byte[] certificateBytes) throws TrustPointAlreadyExistsException,
			CertificateException, InvalidCronExpressionException;

	/**
	 * Sets the specified {@link TrustDomainEntity} as default.
	 * 
	 * @param trustDomain
	 */
	void setDefault(TrustDomainEntity trustDomain);

	/**
	 * Removes the selected {@link TrustPointEntity} and all related
	 * {@link CertificateAuthorityEntity}'s. Removes existing {@link Timer}'s.
	 * 
	 * @param trustPoint
	 */
	void removeTrustPoint(TrustPointEntity trustPoint);

	/**
	 * Finds the {@link TrustPointEntity} from the specified name. Returns
	 * <code>null</code> if not found.
	 * 
	 * @param name
	 */
	TrustPointEntity findTrustPoint(String name);

	/**
	 * Sets the {@link TrustPointEntity}'s for the specified
	 * {@link TrustDomainEntity}.
	 * 
	 * @param trustDomain
	 * @param trustPointNames
	 * 
	 * @throws TrustDomainNotFoundException
	 */
	void setTrustPoints(TrustDomainEntity trustDomain,
			List<String> trustPointNames) throws TrustDomainNotFoundException;

	/**
	 * Add a new {@link PolicyConstraintEntity} to the specified
	 * {@link TrustDomainEntity}.
	 * 
	 * @param trustDomain
	 * @param policy
	 * @return the persisted {@link PolicyConstraintEntity}
	 */
	PolicyConstraintEntity addCertificatePolicy(TrustDomainEntity trustDomain,
			String policy);

	/**
	 * Remove the specified {@link PolicyConstraintEntity}.
	 * 
	 * @param certificatePolicy
	 */
	void removeCertificatePolicy(PolicyConstraintEntity certificatePolicy);

	/**
	 * Add a new {@link KeyUsageConstraintEntity} to the specified
	 * {@link TrustDomainEntity}.
	 * 
	 * @param trustDomain
	 * @param keyUsage
	 * @param allowed
	 * @return the persisted {@link KeyUsageConstraintEntity}
	 */
	KeyUsageConstraintEntity addKeyUsageConstraint(
			TrustDomainEntity trustDomain, KeyUsageType keyUsage,
			boolean allowed);

	/**
	 * Remove the specified {@link KeyUsageConstraintEntity}.
	 * 
	 * @param keyUsageConstraint
	 */
	void removeKeyUsageConstraint(KeyUsageConstraintEntity keyUsageConstraint);

	/**
	 * Save the specified {@link KeyUsageConstraintEntity}'s.
	 * 
	 * @param keyUsageConstraints
	 */
	void saveKeyUsageConstraints(
			List<KeyUsageConstraintEntity> keyUsageConstraints);

	/**
	 * Add a new {@link EndEntityConstraintEntity} to the specified
	 * {@link TrustDomainEntity}.
	 * 
	 * @param trustDomain
	 * @param certificateBytes
	 * @return the persisted {@link EndEntityConstraintEntity}
	 * @throws CertificateException
	 */
	EndEntityConstraintEntity addEndEntityConstraint(
			TrustDomainEntity trustDomain, byte[] certificateBytes)
			throws CertificateException;

	/**
	 * Remove the specified {@link EndEntityConstraintEntity}.
	 * 
	 * @param keyUsageConstraint
	 */
	void removeEndEntityConstraint(EndEntityConstraintEntity endEntityConstraint);

	QCStatementsConstraintEntity addQCConstraint(TrustDomainEntity trustDomain,
			boolean qc);

	void removeQCConstraint(QCStatementsConstraintEntity qcConstraint);

	void saveQCConstraint(QCStatementsConstraintEntity qcConstraint);

	DNConstraintEntity addDNConstraint(TrustDomainEntity trustDomain, String dn);

	void removeDNConstraint(DNConstraintEntity dnConstraint);

	void saveDNConstraint(DNConstraintEntity dnConstraint);

	/**
	 * Add a new {@link TrustDomainEntity}.
	 * 
	 * @param name
	 */
	TrustDomainEntity addTrustDomain(String name);

	/**
	 * Remove the specified {@link TrustDomainEntity}.
	 * 
	 * @param trustDomain
	 */
	void removeTrustDomain(TrustDomainEntity trustDomain);

}
