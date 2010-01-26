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
import java.security.cert.CertificateEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.TimerHandle;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "scheduling")
public class SchedulingEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;

	private String cronExpression;
	private TimerHandle timerHandle;
	private Date fireDate;

	private List<TrustDomainEntity> trustDomains;

	/**
	 * Default constructor.
	 */
	public SchedulingEntity() {
		super();
	}

	/**
	 * Main constructor.
	 * 
	 * @param name
	 * @param crlUrl
	 * @param certificate
	 * @throws CertificateEncodingException
	 */
	public SchedulingEntity(String name, String cronExpression) {
		this.name = name;
		this.cronExpression = cronExpression;
		this.trustDomains = new LinkedList<TrustDomainEntity>();
	}

	@Id
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCronExpression() {

		return cronExpression;
	}

	public void setCronExpression(String cronExpression) {

		this.cronExpression = cronExpression;
	}

	@OneToMany(mappedBy = "scheduling", fetch = FetchType.EAGER)
	public List<TrustDomainEntity> getTrustDomains() {

		return trustDomains;
	}

	public void setTrustDomains(List<TrustDomainEntity> trustDomains) {

		this.trustDomains = trustDomains;
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
