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

import javax.ejb.TimerHandle;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "trust_domain")
@NamedQueries( {
		@NamedQuery(name = TrustDomainEntity.QUERY_LIST_ALL, query = "FROM TrustDomainEntity"),
		@NamedQuery(name = TrustDomainEntity.QUERY_GET_DEFAULT, query = "SELECT td FROM TrustDomainEntity AS td "
				+ "WHERE td.defaultDomain = true") })
public class TrustDomainEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String QUERY_LIST_ALL = "td.list.all";
	public static final String QUERY_GET_DEFAULT = "td.get.default";

	private String name;

	private boolean defaultDomain = false;

	private String crlRefreshCron;
	private TimerHandle timerHandle;
	private Date fireDate;

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
	 * @param crlRefreshCron
	 */
	public TrustDomainEntity(String name, String crlRefreshCron) {
		this.name = name;
		this.crlRefreshCron = crlRefreshCron;
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
