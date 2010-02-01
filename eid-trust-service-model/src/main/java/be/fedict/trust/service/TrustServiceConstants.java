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

/**
 * Some general constants.
 * 
 * @author wvdhaute
 * 
 */
public abstract class TrustServiceConstants {

	public static final String JNDI_CONTEXT = "TrustService";

	public static final String ADMIN_SECURITY_DOMAIN = "trust-service-admin";
	public static final String ADMIN_ROLE = "admin";

	public static final String BELGIAN_EID_TRUST_DOMAIN = "belgian.eid";
	public static final String BELGIAN_EID_ROOT_CA_TRUST_POINT = "Belgian eID Root CA";
	public static final String BELGIAN_EID_ROOT_CA2_TRUST_POINT = "Belgian eID Root CA2";

	public static final String DEFAULT_CRON = "0 0/10 * * * ?";
}
