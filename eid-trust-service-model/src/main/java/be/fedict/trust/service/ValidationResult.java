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

import be.fedict.trust.RevocationData;
import be.fedict.trust.TrustLinkerResult;
import be.fedict.trust.TrustLinkerResultReason;

/**
 * Validation result wrapper.
 * 
 * Contains the result and the optional {@link RevocationData} if requested.
 * 
 * @author wvdhaute
 * 
 */
public class ValidationResult {

	private final TrustLinkerResult result;
	private final RevocationData revocationData;

	public ValidationResult(TrustLinkerResult result,
			RevocationData revocationData) {

		this.result = result;
		this.revocationData = revocationData;
	}

	public boolean isValid() {

		return this.result.isValid();
	}

	public TrustLinkerResultReason getReason() {

		return this.result.getReason();
	}

	public RevocationData getRevocationData() {

		return this.revocationData;
	}
}
