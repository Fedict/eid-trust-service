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
		TRUST_DOMAIN, TRUST_POINT;
	}

	private final Type type;

	private final String name;

	public TimerInfo(Type type, String name) {
		this.type = type;
		this.name = name;
	}

	public Type getType() {

		return this.type;
	}

	public String getName() {

		return this.name;
	}
}