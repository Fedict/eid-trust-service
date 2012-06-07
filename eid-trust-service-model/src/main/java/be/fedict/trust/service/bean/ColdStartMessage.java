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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

public class ColdStartMessage implements JMSMessage {

	private static final long serialVersionUID = 1L;

	private static final String CRL_URL_PROPERTY = "crlUrl";

	private static final String CERT_URL_PROPERTY = "certUrl";

	private String crlUrl;

	private String certUrl;

	public ColdStartMessage(String crlUrl, String certUrl) {
		this.crlUrl = crlUrl;
		this.certUrl = certUrl;
	}

	public ColdStartMessage(Message message) throws JMSException {
		this.crlUrl = message.getStringProperty(CRL_URL_PROPERTY);
		this.certUrl = message.getStringProperty(CERT_URL_PROPERTY);
	}

	public Message getJMSMessage(Session session) throws JMSException {
		Message message = session.createMessage();
		message.setStringProperty(CRL_URL_PROPERTY, this.crlUrl);
		message.setStringProperty(CERT_URL_PROPERTY, this.certUrl);
		return message;
	}

	public String getCrlUrl() {
		return this.crlUrl;
	}

	public String getCertUrl() {
		return this.certUrl;
	}
}
