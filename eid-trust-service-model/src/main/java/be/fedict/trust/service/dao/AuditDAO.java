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

package be.fedict.trust.service.dao;

import java.util.List;

import javax.ejb.Local;

import be.fedict.trust.service.entity.AuditEntity;

/**
 * Audit DAO.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface AuditDAO {

	public static final String JNDI_BINDING = "fedict/eid/trust/AuditDAOBean";

	/**
	 * Returns list of logged audit events.
	 */
	List<AuditEntity> listAudits();

	/**
	 * Log new {@link AuditEntity} with specified message.
	 */
	AuditEntity logAudit(String message);

	/**
	 * Clears all logged audit events.
	 */
	void clearAudits();
}
