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

/**
 * Remove CA JMS Message marshaller/unmarshaller.
 * 
 * @author Frank Cornelis
 * 
 */
public class RemoveCAMessage implements JMSMessage {

	private static final long serialVersionUID = 1L;

	private static final String CA_NAME_PROPERTY = "caName";

	private String caName;

	public RemoveCAMessage(String caName) {
		this.caName = caName;
	}

	public RemoveCAMessage(Message message) throws JMSException {
		this.caName = message.getStringProperty(CA_NAME_PROPERTY);
	}

	public Message getJMSMessage(Session session) throws JMSException {
		Message message = session.createMessage();
		message.setStringProperty(CA_NAME_PROPERTY, this.caName);
		return message;
	}

	public String getCAName() {
		return this.caName;
	}
}
