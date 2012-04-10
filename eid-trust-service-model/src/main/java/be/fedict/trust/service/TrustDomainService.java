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
import javax.jms.JMSException;

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
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.exception.TrustDomainAlreadyExistsException;
import be.fedict.trust.service.exception.TrustDomainNotFoundException;
import be.fedict.trust.service.exception.TrustPointAlreadyExistsException;
import be.fedict.trust.service.exception.VirtualTrustDomainAlreadyExistsException;
import be.fedict.trust.service.exception.VirtualTrustDomainNotFoundException;

/**
 * Trust domain service.
 * 
 * @author wvdhaute
 */
@Local
public interface TrustDomainService {

	/**
	 * List all {@link TrustDomainEntity}'s.
	 */
	List<TrustDomainEntity> listTrustDomains();

	/**
	 * List all {@link VirtualTrustDomainEntity}'s.
	 */
	List<VirtualTrustDomainEntity> listVirtualTrustDomains();

	/**
	 * List all {@link TrustPointEntity}'s.
	 */
	List<TrustPointEntity> listTrustPoints();

	/**
	 * List all {@link CertificateAuthorityEntity}'s related to the specified
	 * {@link TrustPointEntity}.
	 * 
	 * @param trustPoint
	 */
	List<CertificateAuthorityEntity> listTrustPointCAs(
			TrustPointEntity trustPoint);

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
	 */
	void save(TrustDomainEntity trustDomain);

	/**
	 * Save the changes to the specified {@link TrustPointEntity}
	 * 
	 * @param trustPoint
	 */
	void save(TrustPointEntity trustPoint)
			throws InvalidCronExpressionException;

	/**
	 * Add a new {@link TrustPointEntity}.
	 * 
	 * @param crlRefreshCronSchedule
	 * @param certificateBytes
	 * @throws TrustPointAlreadyExistsException
	 * 
	 * @throws CertificateException
	 */
	TrustPointEntity addTrustPoint(String crlRefreshCronSchedule,
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
	 * @throws TrustDomainNotFoundException
	 */
	void setTrustPoints(TrustDomainEntity trustDomain,
			List<String> trustPointNames) throws TrustDomainNotFoundException;

	/**
	 * Sets the {@link TrustDomainEntity}'s for the specified
	 * {@link VirtualTrustDomainEntity}.
	 * 
	 * @param virtualTrustDomain
	 * @param trustDomainNames
	 * @throws VirtualTrustDomainNotFoundException
	 * 
	 */
	VirtualTrustDomainEntity setTrustDomains(
			VirtualTrustDomainEntity virtualTrustDomain,
			List<String> trustDomainNames)
			throws VirtualTrustDomainNotFoundException;

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
	 * Save the specified {@link KeyUsageConstraintEntity}'s.
	 */
	void saveKeyUsageConstraints(
			List<KeyUsageConstraintEntity> keyUsageConstraints);

	/**
	 * Add a new {@link EndEntityConstraintEntity} to the specified
	 * {@link TrustDomainEntity}.
	 */
	EndEntityConstraintEntity addEndEntityConstraint(
			TrustDomainEntity trustDomain, byte[] certificateBytes)
			throws CertificateException;

	/**
	 * Add a new {@link QCStatementsConstraintEntity} to the specified
	 * {@link TrustDomainEntity}.
	 */
	QCStatementsConstraintEntity addQCConstraint(TrustDomainEntity trustDomain,
			boolean qc);

	/**
	 * Save the specified {@link QCStatementsConstraintEntity}.
	 */
	void saveQCConstraint(QCStatementsConstraintEntity qcConstraint);

	/**
	 * Add a new {@link DNConstraintEntity} to the specified
	 * {@link TrustDomainEntity}.
	 */
	DNConstraintEntity addDNConstraint(TrustDomainEntity trustDomain, String dn);

	/**
	 * Save the specified {@link DNConstraintEntity}.
	 */
	void saveDNConstraint(DNConstraintEntity dnConstraint);

	/**
	 * Add a new {@link TSAConstraintEntity} to the specified
	 * {@link TrustDomainEntity}.
	 */
	TSAConstraintEntity addTSAConstraint(TrustDomainEntity trustDomain);

	/**
	 * Remove the specified {@link CertificateConstraintEntity}.
	 */
	void removeCertificateConstraint(
			CertificateConstraintEntity certificateConstraint);

	/**
	 * Add a new {@link TrustDomainEntity}.
	 * 
	 * @param name
	 * @throws TrustDomainAlreadyExistsException
	 * 
	 */
	TrustDomainEntity addTrustDomain(String name)
			throws TrustDomainAlreadyExistsException;

	/**
	 * Add a new {@link VirtualTrustDomainEntity}.
	 * 
	 * @param name
	 * @throws VirtualTrustDomainAlreadyExistsException
	 * 
	 * @throws TrustDomainAlreadyExistsException
	 * 
	 */
	VirtualTrustDomainEntity addVirtualTrustDomain(String name)
			throws VirtualTrustDomainAlreadyExistsException,
			TrustDomainAlreadyExistsException;

	/**
	 * Remove the specified {@link TrustDomainEntity}.
	 * 
	 * @param trustDomain
	 */
	void removeTrustDomain(TrustDomainEntity trustDomain);

	/**
	 * Remove the specified {@link VirtualTrustDomainEntity}.
	 * 
	 * @param virtualTrustDomain
	 */
	void removeVirtualTrustDomain(VirtualTrustDomainEntity virtualTrustDomain);

	/**
	 * Refresh the specified {@link TrustPointEntity}'s revocation cache
	 * immediately.
	 * 
	 * @param trustPoint
	 */
	void refreshTrustPointCache(TrustPointEntity trustPoint);

	/**
	 * Refresh the specified {@link CertificateAuthorityEntity}'s revocation
	 * cache immediately.
	 * 
	 * @param ca
	 * @throws JMSException
	 */
	void refreshCACache(CertificateAuthorityEntity ca) throws JMSException;
}
