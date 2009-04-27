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

import java.util.List;

import javax.jws.WebService;
import javax.xml.bind.JAXBElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3._2000._09.xmldsig_.KeyInfoType;
import org.w3._2000._09.xmldsig_.X509DataType;
import org.w3._2002._03.xkms_.QueryKeyBindingType;
import org.w3._2002._03.xkms_.ValidateRequestType;
import org.w3._2002._03.xkms_.ValidateResultType;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

/**
 * Implementation of XKMS2 Web Service JAX-WS Port.
 * 
 * @author fcorneli
 * 
 */
@WebService(endpointInterface = "org.w3._2002._03.xkms_wsdl.XKMSPortType")
public class XKMSPortImpl implements XKMSPortType {

	private static final Log LOG = LogFactory.getLog(XKMSPortImpl.class);

	public ValidateResultType validate(ValidateRequestType body) {
		LOG.debug("validate");
		// parse the request
		QueryKeyBindingType queryKeyBinding = body.getQueryKeyBinding();
		KeyInfoType keyInfo = queryKeyBinding.getKeyInfo();
		List<Object> keyInfoContent = keyInfo.getContent();
		for (Object keyInfoObject : keyInfoContent) {
			JAXBElement<?> keyInfoElement = (JAXBElement<?>) keyInfoObject;
			Object elementValue = keyInfoElement.getValue();
			if (elementValue instanceof X509DataType) {
				X509DataType x509Data = (X509DataType) elementValue;
				List<Object> x509DataContent = x509Data
						.getX509IssuerSerialOrX509SKIOrX509SubjectName();
				// TODO
			}
		}
		// process the request

		// return the result
		return null;
	}
}
