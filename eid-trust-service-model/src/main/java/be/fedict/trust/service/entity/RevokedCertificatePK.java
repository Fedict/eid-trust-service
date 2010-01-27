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
import java.math.BigInteger;

import javax.persistence.Embeddable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@Embeddable
public class RevokedCertificatePK implements Serializable {

	private static final long serialVersionUID = 1L;

	private String issuer;

	private String serialNumber;

	public RevokedCertificatePK() {

		// empty
	}

	public RevokedCertificatePK(String issuer, BigInteger serialNumber) {
		this.issuer = issuer;
		this.serialNumber = serialNumber.toString();
	}

	public String getIssuer() {
		return this.issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	/**
	 * HSQLDB/Hibernate has problems with mapping a BigInteger correctly.
	 * 
	 * @return
	 */
	public String getSerialNumber() {
		return this.serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	@Override
	public boolean equals(Object obj) {

		if (null == obj)
			return false;
		if (this == obj)
			return true;
		if (false == obj instanceof RevokedCertificatePK)
			return false;
		RevokedCertificatePK rhs = (RevokedCertificatePK) obj;
		return new EqualsBuilder().append(serialNumber, rhs.serialNumber)
				.append(issuer, rhs.issuer).isEquals();
	}

	@Override
	public int hashCode() {

		return new HashCodeBuilder().append(serialNumber).append(issuer)
				.toHashCode();
	}

	@Override
	public String toString() {

		return new ToStringBuilder(this).append("serialNumber", serialNumber)
				.append("issuer", issuer).toString();
	}

}
