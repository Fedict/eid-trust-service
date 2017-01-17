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

package be.fedict.trust.client.exception;

import java.util.List;

public class ValidationFailedException extends Exception {

	private static final long serialVersionUID = 1L;

	private final List<String> reasonURIs;

	public ValidationFailedException(List<String> reasonURIs) {
		this.reasonURIs = reasonURIs;
	}

	/**
	 * Returns the XKMS v2.0 reason URIs for the failed validation.
	 * 
	 * @see <a href="http://www.w3.org/TR/xkms2/#XKMS_2_0_Section_5_1">XKMS
	 *      2.0</a>
	 */
	public List<String> getReasons() {
		return this.reasonURIs;
	}
}
