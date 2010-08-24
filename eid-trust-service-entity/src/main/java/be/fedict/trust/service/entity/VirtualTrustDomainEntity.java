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

package be.fedict.trust.service.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "ts_virt_trust_domain")
@NamedQueries( {
		@NamedQuery(name = VirtualTrustDomainEntity.QUERY_LIST_ALL, query = "FROM VirtualTrustDomainEntity"),
		@NamedQuery(name = VirtualTrustDomainEntity.QUERY_LIST_TRUST_DOMAINS, query = "SELECT vtd.trustDomains "
				+ "FROM VirtualTrustDomainEntity AS vtd WHERE vtd.name = :name") })
public class VirtualTrustDomainEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String QUERY_LIST_ALL = "ts_vtd.list.all";
	public static final String QUERY_LIST_TRUST_DOMAINS = "ts_vtd.list.td";

	private String name;

	// trust domains
	private Set<TrustDomainEntity> trustDomains;

	/**
	 * Default constructor.
	 */
	public VirtualTrustDomainEntity() {
		super();
		this.trustDomains = new HashSet<TrustDomainEntity>();
	}

	/**
	 * Main constructor.
	 * 
	 * @param name
	 * @param crlRefreshCron
	 */
	public VirtualTrustDomainEntity(String name) {

		this.name = name;
		this.trustDomains = new HashSet<TrustDomainEntity>();
	}

	@Id
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToMany(fetch = FetchType.EAGER)
	public Set<TrustDomainEntity> getTrustDomains() {

		return this.trustDomains;
	}

	public void setTrustDomains(Set<TrustDomainEntity> trustDomains) {

		this.trustDomains = trustDomains;
	}
}
