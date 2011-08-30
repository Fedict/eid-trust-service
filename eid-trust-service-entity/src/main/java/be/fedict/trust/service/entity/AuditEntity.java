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

package be.fedict.trust.service.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "ts_audit")
@NamedQueries({
		@NamedQuery(name = AuditEntity.QUERY_LIST_ALL, query = "FROM AuditEntity"),
		@NamedQuery(name = AuditEntity.REMOVE_ALL, query = "DELETE FROM AuditEntity") })
public class AuditEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String QUERY_LIST_ALL = "ts_audit.all";
	public static final String REMOVE_ALL = "ts_audit.rem.all";

	private long id;

	private Date auditDate;
	private String message;

	/**
	 * Default constructor.
	 */
	public AuditEntity() {
		super();
	}

	/**
	 * Main constructor.
	 */
	public AuditEntity(Date auditDate, String message) {

		this.auditDate = auditDate;
		this.message = message;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {

		return this.id;
	}

	public void setId(long id) {

		this.id = id;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getAuditDate() {

		return this.auditDate;
	}

	public void setAuditDate(Date auditDate) {

		this.auditDate = auditDate;
	}

	public String getMessage() {

		return this.message;
	}

	public void setMessage(String message) {

		this.message = message;
	}
}
