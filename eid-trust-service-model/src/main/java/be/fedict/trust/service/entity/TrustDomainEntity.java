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

package be.fedict.trust.service.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "trust_domain")
public class TrustDomainEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;

	private SchedulingEntity scheduling;

	/**
	 * Default constructor.
	 */
	public TrustDomainEntity() {
		super();
	}

	/**
	 * Main constructor.
	 * 
	 * @param name
	 * @param scheduling
	 */
	public TrustDomainEntity(String name, SchedulingEntity scheduling) {
		this.name = name;
		this.scheduling = scheduling;
	}

	@Id
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	public SchedulingEntity getScheduling() {

		return scheduling;
	}

	public void setScheduling(SchedulingEntity schedulingEntity) {

		scheduling = schedulingEntity;
	}

}
