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

package be.fedict.trust.xkms2;

import java.util.HashMap;
import java.util.Map;

/**
 * XKMS2 ResultMinor codes.
 * 
 * @author wvdhaute
 * 
 */
public enum ResultMinorCode {

	NO_MATCH("http://www.w3.org/2002/03/xkms#NoMatch"), TOO_MANY_RESPONSES(
			"http://www.w3.org/2002/03/xkms#TooManyResponses"), INCOMPLETE(
			"http://www.w3.org/2002/03/xkms#Incomplete"), FAILURE(
			"http://www.w3.org/2002/03/xkms#Failure"), REFUSED(
			"http://www.w3.org/2002/03/xkms#Refused"), NO_AUTHENTICATION(
			"http://www.w3.org/2002/03/xkms#NoAuthentication"), MESSAGE_NOT_SUPPORTED(
			"http://www.w3.org/2002/03/xkms#MessageNotSupported"), UNKNOWN_RESPONSE_ID(
			"http://www.w3.org/2002/03/xkms#UnknownResponseId"), REPRESENT_REQUIRED(
			"http://www.w3.org/2002/03/xkms#RepresentRequired"), NOT_SYNCHRONOUS(
			"http://www.w3.org/2002/03/xkms#NotSynchronous"), OPTIONAL_ELEMENT_NOT_SUPPORTED(
			"http://www.w3.org/2002/03/xkms#OptionalElementNotSupported"), PROOF_OF_POSSESSION_REQUIRED(
			"http://www.w3.org/2002/03/xkms#ProofOfPossessionRequired"), TIME_INSTANT_NOT_SUPPORTED(
			"http://www.w3.org/2002/03/xkms#TimeInstantNotSupported"), TIME_INSTANT_OUT_OF_RANGE(
			"http://www.w3.org/2002/03/xkms#TimeInstantOutOfRange"), TRUST_DOMAIN_NOT_FOUND(
			"urn:be:fedict:trust:TrustDomainNotFound");

	private final String errorCode;

	private final static Map<String, ResultMinorCode> errorCodeMap = new HashMap<String, ResultMinorCode>();

	static {
		ResultMinorCode[] errorCodes = ResultMinorCode.values();
		for (ResultMinorCode errorCode : errorCodes) {
			errorCodeMap.put(errorCode.getErrorCode(), errorCode);
		}
	}

	private ResultMinorCode(String errorCode) {

		this.errorCode = errorCode;
	}

	public String getErrorCode() {

		return errorCode;
	}

	public static ResultMinorCode getResultMinorCode(String errorCode) {

		ResultMinorCode resultMinorCode = errorCodeMap.get(errorCode);
		if (null == resultMinorCode)
			throw new IllegalArgumentException(
					"unknown ResultMinor error code: " + errorCode);
		return resultMinorCode;
	}

}
