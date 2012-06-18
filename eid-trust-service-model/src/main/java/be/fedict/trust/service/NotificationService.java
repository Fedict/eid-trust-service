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

package be.fedict.trust.service;

import javax.ejb.Local;
import javax.jms.JMSException;

/**
 * Interface for notification service component. Used for the async
 * communication towards different background processes.
 * 
 * @author Frank Cornelis
 * 
 */
@Local
public interface NotificationService {

	void notifyDownloader(String issuerName, boolean update)
			throws JMSException;

	void notifyHarvester(String issuerName, String crlFile, boolean update)
			throws JMSException;

	void notifyColdStart(String crlUrl, String certUrl) throws JMSException;

	void notifyRemoveCA(String issuerName) throws JMSException;
}
