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

package be.fedict.trust.service;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.entity.TrustPointEntity;

/**
 * Contains information for the {@link SchedulingService} as to for who the
 * timeout is.
 * 
 * @author wvdhaute
 * 
 */
public class TimerInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum Type {
		TRUST_POINT, CLOCK_DRIFT;
	}

	private final Type type;

	private final String name;

	public TimerInfo(TrustPointEntity trustPoint) {
		this.type = Type.TRUST_POINT;
		this.name = trustPoint.getName();
	}

	public TimerInfo(ClockDriftConfigEntity clockDriftDetectionConfig) {
		this.type = Type.CLOCK_DRIFT;
		this.name = clockDriftDetectionConfig.getName();
	}

	public Type getType() {

		return this.type;
	}

	public String getName() {

		return this.name;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}
		if (null == obj) {
			return false;
		}
		if (false == obj instanceof TimerInfo) {
			return false;
		}
		TimerInfo rhs = (TimerInfo) obj;
		return new EqualsBuilder().append(this.type, rhs.type).append(
				this.name, rhs.name).isEquals();

	}

	@Override
	public int hashCode() {

		return new HashCodeBuilder().append(this.type).append(this.name)
				.toHashCode();
	}

	@Override
	public String toString() {

		return new ToStringBuilder(this).append("type", this.type).append(
				"name", this.name).toString();
	}
}