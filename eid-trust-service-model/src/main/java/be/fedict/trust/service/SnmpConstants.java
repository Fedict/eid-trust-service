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
 * Contains the SNMP counters' OID's
 * 
 * @author wvdhaute
 * 
 */
public abstract class SnmpConstants {

	private static final String SNMP_OID_ROOT = "1.3.6.1.4.1.7890.";

	public static final String VALIDATE = SNMP_OID_ROOT + "0.0";
	public static final String CACHE_REFRESH = SNMP_OID_ROOT + "1.0";
	public static final String CACHE_HITS = SNMP_OID_ROOT + "2.0";
	public static final String CACHE_MISSES = SNMP_OID_ROOT + "3.0";
	public static final String CACHE_HIT_PERCENTAGE = SNMP_OID_ROOT + "4.0";
	public static final String CRL_DOWNLOAD_FAILURES = SNMP_OID_ROOT + "5.0";
	public static final String OCSP_FAILURES = SNMP_OID_ROOT + "6.0";

}
