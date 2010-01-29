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
import javax.persistence.Table;

@Entity
@Table(name = "trust_domain")
public class TrustDomainEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;

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

		return crlRefreshCron;
	}

	public void setCrlRefreshCron(String crlRefreshCron) {

		this.crlRefreshCron = crlRefreshCron;
	}

	@Lob
	public TimerHandle getTimerHandle() {

		return timerHandle;
	}

	public void setTimerHandle(TimerHandle timerHandle) {

		this.timerHandle = timerHandle;
	}

	public Date getFireDate() {

		return fireDate;
	}

	public void setFireDate(Date fireDate) {

		this.fireDate = fireDate;
	}

}
