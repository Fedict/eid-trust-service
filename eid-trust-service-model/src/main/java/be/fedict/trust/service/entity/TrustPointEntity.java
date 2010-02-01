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
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "trust_point")
public class TrustPointEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;

	private String crlRefreshCron;
	private TimerHandle timerHandle;
	private Date fireDate;

	private TrustDomainEntity trustDomain;

	private CertificateAuthorityEntity certificateAuthority;

	/**
	 * Default constructor.
	 */
	public TrustPointEntity() {
		super();
	}

	/**
	 * Main constructor.
	 * 
	 * @param name
	 * @param crlRefreshCron
	 * @param trustDomain
	 * @param certificateAuthority
	 */
	public TrustPointEntity(String name, String crlRefreshCron,
			TrustDomainEntity trustDomain,
			CertificateAuthorityEntity certificateAuthority) {
		this.name = name;
		this.trustDomain = trustDomain;
		this.certificateAuthority = certificateAuthority;
		this.crlRefreshCron = crlRefreshCron;
	}

	@Id
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne(optional = false)
	public TrustDomainEntity getTrustDomain() {

		return this.trustDomain;
	}

	public void setTrustDomain(TrustDomainEntity trustDomain) {

		this.trustDomain = trustDomain;
	}

	public CertificateAuthorityEntity getCertificateAuthority() {

		return this.certificateAuthority;
	}

	public void setCertificateAuthority(
			CertificateAuthorityEntity certificateAuthority) {

		this.certificateAuthority = certificateAuthority;
	}

	public String getCrlRefreshCron() {

		return this.crlRefreshCron;
	}

	public void setCrlRefreshCron(String crlRefreshCron) {

		this.crlRefreshCron = crlRefreshCron;
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
