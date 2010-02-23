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

package be.fedict.trust.admin.portal;

import java.io.IOException;

import javax.ejb.Local;

import org.richfaces.event.UploadEvent;

@Local
public interface TrustPoint {

	/*
	 * Factory
	 */
	void trustDomainListFactory();

	/*
	 * Lifecycle.
	 */
	void destroyCallback();

	/*
	 * Accessors
	 */
	String getCrlRefreshCron();

	void setCrlRefreshCron(String crlRefreshCron);

	/*
	 * Actions
	 */
	String modify();

	String save();

	String remove();

	String add();

	String back();

	/*
	 * Richfaces component callbacks
	 */
	void uploadListener(UploadEvent event) throws IOException;
}
