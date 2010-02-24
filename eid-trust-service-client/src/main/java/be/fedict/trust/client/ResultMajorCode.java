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

package be.fedict.trust.client;

import java.util.HashMap;
import java.util.Map;

/**
 * XKMS2 ResultMajor codes.
 * 
 * @author wvdhaute
 * 
 */
public enum ResultMajorCode {

	SUCCESS("http://www.w3.org/2002/03/xkms#Success"), VERSION_MISMATCH(
			"http://www.w3.org/2002/03/xkms#VersionMismatch"), SENDER(
			"http://www.w3.org/2002/03/xkms#Sender"), RECEIVER(
			"http://www.w3.org/2002/03/xkms#Receiver"), REPRESENT(
			"http://www.w3.org/2002/03/xkms#Represent"), PENDING(
			"http://www.w3.org/2002/03/xkms#Pending");

	private final String errorCode;

	private final static Map<String, ResultMajorCode> errorCodeMap = new HashMap<String, ResultMajorCode>();

	static {
		ResultMajorCode[] errorCodes = ResultMajorCode.values();
		for (ResultMajorCode errorCode : errorCodes) {
			errorCodeMap.put(errorCode.getErrorCode(), errorCode);
		}
	}

	private ResultMajorCode(String errorCode) {

		this.errorCode = errorCode;
	}

	public String getErrorCode() {

		return errorCode;
	}

	public static ResultMajorCode getResultMajorCode(String errorCode) {

		ResultMajorCode resultMajorCode = errorCodeMap.get(errorCode);
		if (null == resultMajorCode)
			throw new IllegalArgumentException(
					"unknown ResultMajor error code: " + errorCode);
		return resultMajorCode;
	}

}
