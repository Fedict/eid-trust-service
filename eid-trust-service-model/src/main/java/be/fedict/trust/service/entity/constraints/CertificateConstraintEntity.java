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

package be.fedict.trust.service.entity.constraints;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import be.fedict.trust.service.entity.TrustDomainEntity;

/**
 * Base entity for all certificate constraints that can be configured per trust
 * domain.
 * 
 * @author wvdhaute
 */
@Entity
@Table(name = "cert_contraint")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class CertificateConstraintEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private long id;
	private TrustDomainEntity trustDomain;

	/**
	 * Default constructor.
	 */
	public CertificateConstraintEntity() {
		super();
	}

	/**
	 * Main constructor.
	 */
	public CertificateConstraintEntity(TrustDomainEntity trustDomain) {

		this.trustDomain = trustDomain;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {

		return this.id;
	}

	public void setId(long id) {

		this.id = id;
	}

	@ManyToOne
	public TrustDomainEntity getTrustDomain() {

		return this.trustDomain;
	}

	public void setTrustDomain(TrustDomainEntity trustDomain) {

		this.trustDomain = trustDomain;
	}

	@Override
	public boolean equals(Object obj) {

		if (null == obj)
			return false;
		if (this == obj)
			return true;
		if (false == obj instanceof CertificateConstraintEntity)
			return false;
		CertificateConstraintEntity rhs = (CertificateConstraintEntity) obj;
		return new EqualsBuilder().append(id, rhs.id).isEquals();
	}

	@Override
	public int hashCode() {

		return new HashCodeBuilder().append(this.id).hashCode();
	}

}
