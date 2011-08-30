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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "ts_admin")
@NamedQueries({ @NamedQuery(name = AdministratorEntity.QUERY_LIST_ALL, query = "FROM AdministratorEntity") })
public class AdministratorEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String QUERY_LIST_ALL = "ts_admin.all";

	private String id;
	private String name;
	private boolean pending;

	/**
	 * Default constructor.
	 */
	public AdministratorEntity() {
		super();
	}

	public AdministratorEntity(String id, String name) {

		this(id, name, false);
	}

	public AdministratorEntity(String id, String name, boolean pending) {
		this.id = id;
		this.name = name;
		this.pending = pending;
	}

	@Id
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return name of the administrator. This is the Subject of the
	 *         authentication certificate
	 */
	@Column(nullable = false)
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return whether or not this is a pending administrator.
	 */
	public boolean isPending() {
		return this.pending;
	}

	public void setPending(boolean pending) {
		this.pending = pending;
	}
}
