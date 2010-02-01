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

package be.fedict.trust.service.bean;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

public class HarvestMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private String caName;

	private boolean update;

	public String getCaName() {
		return this.caName;
	}

	public void setCaName(String caName) {
		this.caName = caName;
	}

	public boolean isUpdate() {
		return this.update;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}

	public HarvestMessage(String caName, boolean update) {

		this.caName = caName;
		this.update = update;
	}

	public HarvestMessage(Message message) throws JMSException {

		this.caName = message.getStringProperty("caName");
		this.update = message.getBooleanProperty("update");
	}

	public Message getJMSMessage(Session session) throws JMSException {

		Message message = session.createMessage();
		message.setStringProperty("caName", this.caName);
		message.setBooleanProperty("update", this.update);
		return message;
	}

}
