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

import javax.persistence.Entity;

import be.fedict.trust.service.entity.TrustDomainEntity;

@Entity
public class DNConstraintEntity extends CertificateConstraintEntity {

	private static final long serialVersionUID = 1L;

	private String dn;

	/**
	 * Default constructor.
	 */
	public DNConstraintEntity() {

		super();
	}

	/**
	 * Main constructor.
	 */
	public DNConstraintEntity(TrustDomainEntity trustDomain, String dn) {

		super(trustDomain);
		this.dn = dn;
	}

	public String getDn() {

		return this.dn;
	}

	public void setDn(String dn) {

		this.dn = dn;
	}
}
