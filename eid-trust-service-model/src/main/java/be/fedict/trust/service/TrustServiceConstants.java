/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2010 FedICT.
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

	public static final String CLOCK_DRIFT_TIMER = "clock-drift-timer";

	public static final String WS_SECURITY_CONFIG = "ws-security-config";
	public static final String NETWORK_CONFIG = "network-config";
	public static final String CLOCK_DRIFT_CONFIG = "clock-drift-config";
	public static final String CLOCK_DRIFT_NTP_SERVER = "0.pool.ntp.org";
	public static final int CLOCK_DRIFT_TIMEOUT = 10 * 1000;
	public static final int CLOCK_DRIFT_MAX_CLOCK_OFFSET = 5 * 1000;

	public static final String BELGIAN_EID_AUTH_TRUST_DOMAIN = "BE-AUTH";
	public static final String BELGIAN_EID_NON_REPUDIATION_TRUST_DOMAIN = "BE";
	public static final String BELGIAN_EID_NATIONAL_REGISTRY_TRUST_DOMAIN = "BE-NAT-REG";

	public static final String DEFAULT_CRON = "0 0 0/3 * * ?";

	public static final String INFO_MESSAGE_KEY = "info";
}
