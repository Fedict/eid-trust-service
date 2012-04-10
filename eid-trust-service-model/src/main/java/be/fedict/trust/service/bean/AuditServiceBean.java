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

package be.fedict.trust.service.bean;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.AuditService;
import be.fedict.trust.service.dao.AuditDAO;
import be.fedict.trust.service.entity.AuditEntity;

/**
 * Administrator Service Bean implementation.
 * 
 * @author wvdhaute
 */
@Stateless
public class AuditServiceBean implements AuditService {

	private static final Log LOG = LogFactory.getLog(AuditServiceBean.class);

	@EJB
	private AuditDAO auditDAO;

	/**
	 * {@inheritDoc}
	 */
	public List<AuditEntity> listAudits() {

		LOG.debug("list audit entries");
		return this.auditDAO.listAudits();
	}

	/**
	 * {@inheritDoc}
	 */
	public void clearAudits() {

		LOG.debug("clear audit entries");
		this.auditDAO.clearAudits();
	}
}
