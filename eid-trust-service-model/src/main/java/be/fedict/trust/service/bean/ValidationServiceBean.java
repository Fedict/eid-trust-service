/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2012 FedICT.
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

package be.fedict.trust.service.bean;

import java.math.BigInteger;

import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.ValidationService;

/**
 * Validation Service implementation.
 * 
 * @author Frank Cornelis
 */
@Stateless
public class ValidationServiceBean implements ValidationService {

	private static final Log LOG = LogFactory
			.getLog(ValidationServiceBean.class);

	public boolean validate(BigInteger serialNumber, byte[] issuerNameHash,
			byte[] issuerKeyHash) {
		LOG.debug("validate");
		// TODO: implement me
		return false;
	}
}
