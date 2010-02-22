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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.TimerHandle;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import be.fedict.trust.service.entity.constraints.CertificateConstraintEntity;

@Entity
@Table(name = "trust_domain")
@NamedQueries( {
		@NamedQuery(name = TrustDomainEntity.QUERY_LIST_ALL, query = "FROM TrustDomainEntity"),
		@NamedQuery(name = TrustDomainEntity.QUERY_GET_DEFAULT, query = "SELECT td FROM TrustDomainEntity AS td "
				+ "WHERE td.defaultDomain = true"),
		@NamedQuery(name = TrustDomainEntity.QUERY_LIST_TRUST_POINTS, query = "SELECT td.trustPoints FROM TrustDomainEntity AS td "
				+ "WHERE td.name = :name") })
public class TrustDomainEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String QUERY_LIST_ALL = "td.list.all";
	public static final String QUERY_GET_DEFAULT = "td.get.default";
	public static final String QUERY_LIST_TRUST_POINTS = "td.list.tp";

	private String name;

	private boolean defaultDomain = false;

	// trust points
	private List<TrustPointEntity> trustPoints;

	// certificate constraints
	private List<CertificateConstraintEntity> certificateConstraints;

	private String crlRefreshCron;
	private TimerHandle timerHandle;
	private Date fireDate;

	/**
	 * Default constructor.
	 */
	public TrustDomainEntity() {
		super();
		this.certificateConstraints = new LinkedList<CertificateConstraintEntity>();
		this.trustPoints = new LinkedList<TrustPointEntity>();
	}

	/**
	 * Main constructor.
	 * 
	 * @param name
	 * @param crlRefreshCron
	 */
	public TrustDomainEntity(String name, String crlRefreshCron) {

		this.name = name;
		this.crlRefreshCron = crlRefreshCron;
		this.certificateConstraints = new LinkedList<CertificateConstraintEntity>();
		this.trustPoints = new LinkedList<TrustPointEntity>();
	}

	@Id
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCrlRefreshCron() {

		return this.crlRefreshCron;
	}

	public void setCrlRefreshCron(String crlRefreshCron) {

		this.crlRefreshCron = crlRefreshCron;
	}

	public boolean isDefaultDomain() {

		return this.defaultDomain;
	}

	public void setDefaultDomain(boolean defaultDomain) {

		this.defaultDomain = defaultDomain;
	}

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
	public List<CertificateConstraintEntity> getCertificateConstraints() {

		return this.certificateConstraints;
	}

	public void setCertificateConstraints(
			List<CertificateConstraintEntity> certificateConstraints) {

		this.certificateConstraints = certificateConstraints;
	}

	@ManyToMany
	public List<TrustPointEntity> getTrustPoints() {

		return this.trustPoints;
	}

	public void setTrustPoints(List<TrustPointEntity> trustPoints) {

		this.trustPoints = trustPoints;
	}

	@Lob
	public TimerHandle getTimerHandle() {

		return this.timerHandle;
	}

	public void setTimerHandle(TimerHandle timerHandle) {

		this.timerHandle = timerHandle;
	}

	public Date getFireDate() {

		return this.fireDate;
	}

	public void setFireDate(Date fireDate) {

		this.fireDate = fireDate;
	}
}
