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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
@Table(name = "trust_point")
@NamedQueries( { @NamedQuery(name = TrustPointEntity.QUERY_WHERE_TRUST_DOMAIN, query = "SELECT tp FROM TrustPointEntity AS tp "
		+ "WHERE tp.trustDomain = :trustDomain") })
public class TrustPointEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String QUERY_WHERE_TRUST_DOMAIN = "tp.q.w.td";

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
	 * @param crlRefreshCron
	 * @param trustDomain
	 * @param certificateAuthority
	 */
	public TrustPointEntity(String crlRefreshCron,
			TrustDomainEntity trustDomain,
			CertificateAuthorityEntity certificateAuthority) {
		this.name = certificateAuthority.getName();
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

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}
		if (null == obj) {
			return false;
		}
		if (false == obj instanceof TrustPointEntity) {
			return false;
		}
		TrustPointEntity rhs = (TrustPointEntity) obj;
		return new EqualsBuilder().append(this.name, rhs.name).isEquals();

	}

	@Override
	public int hashCode() {

		return new HashCodeBuilder().append(this.name).toHashCode();
	}

	@Override
	public String toString() {

		return new ToStringBuilder(this).append("name", this.name).append(
				"trustDomain", this.trustDomain.getName()).append("crlRefresh",
				this.crlRefreshCron).toString();
	}

}
