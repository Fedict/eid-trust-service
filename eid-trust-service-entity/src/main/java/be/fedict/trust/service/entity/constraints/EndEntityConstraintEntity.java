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

import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;

import be.fedict.trust.service.entity.TrustDomainEntity;

@Entity
public class EndEntityConstraintEntity extends CertificateConstraintEntity {

	private static final long serialVersionUID = 1L;

	private String issuerName;
	private BigInteger serialNumber;

	/**
	 * Default constructor.
	 */
	public EndEntityConstraintEntity() {

		super();
	}

	/**
	 * Main constructor.
	 */
	public EndEntityConstraintEntity(TrustDomainEntity trustDomain,
			String issuerName, BigInteger serialNumber) {

		super(trustDomain);
		this.issuerName = issuerName;
		this.serialNumber = serialNumber;
	}

	public String getIssuerName() {

		return this.issuerName;
	}

	public void setIssuerName(String issuerName) {

		this.issuerName = issuerName;
	}

	@Column(precision = 38)
	public BigInteger getSerialNumber() {

		return this.serialNumber;
	}

	public void setSerialNumber(BigInteger serialNumber) {

		this.serialNumber = serialNumber;
	}
}
