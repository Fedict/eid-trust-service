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

package be.fedict.trust.service.snmp.mbean;

public interface SNMPServiceMBean {

	/**
	 * Increment the SNMP {@link Counter} with specified OID. If not yet
	 * registered, does so.
	 * 
	 * @param oid
	 * @param increment
	 */
	public void increment(String oid, Long increment);

	/**
	 * Returns the current value of the SNMP {@link Counter} associated with the
	 * specified OID. If not yet created and registered, does so.
	 * 
	 * @param oid
	 */
	public Long getValue(String oid);

	/**
	 * Sets the value of the SNMP {@link Counter} associated with the specified
	 * OID. If not yet created and registered, does so.
	 * 
	 * @param oid
	 * @param value
	 */
	public void setValue(String oid, Long value);

	/**
	 * Attributes
	 */
	String getAddress();

	void setAddress(String address);

}
