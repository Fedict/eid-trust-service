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

package be.fedict.trust.xkms2;

public abstract class XKMSConstants {

	public static final String TRUST_DOMAIN_APPLICATION_URI = "urn:be:fedict:trust:trust-domain";
	public static final String TSA_APPLICATION_URI = "urn:be:fedict:trust:tsa";

	public static final String RETURN_REVOCATION_DATA_URI = "urn:be:fedict:trust:revocation-data";

	public static final String KEY_BINDING_STATUS_VALID_URI = "http://www.w3.org/2002/03/xkms#Valid";
	public static final String KEY_BINDING_STATUS_INVALID_URI = "http://www.w3.org/2002/03/xkms#Invalid";
	public static final String KEY_BINDING_STATUS_INDETERMINATE_URI = "http://www.w3.org/2002/03/xkms#Indeterminate";

	public static final String KEY_BINDING_REASON_ISSUER_TRUST_URI = "http://www.w3.org/2002/03/xkms#IssuerTrust";
	public static final String KEY_BINDING_REASON_REVOCATION_STATUS_URI = "http://www.w3.org/2002/03/xkms#RevocationStatus";
	public static final String KEY_BINDING_REASON_VALIDITY_INTERVAL_URI = "http://www.w3.org/2002/03/xkms#ValidityInterval";
	public static final String KEY_BINDING_REASON_SIGNATURE_URI = "http://www.w3.org/2002/03/xkms#Signature";
}
