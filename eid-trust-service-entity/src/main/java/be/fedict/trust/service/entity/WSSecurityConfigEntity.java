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

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "wssec_config")
public class WSSecurityConfigEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;

	private boolean signing;

	private KeyStoreType keyStoreType;
	private String keyStorePath;
	private String keyStorePassword;
	private String keyEntryPassword;
	private String alias;

	/**
	 * Default constructor.
	 */
	public WSSecurityConfigEntity() {
		super();
	}

	/**
	 * Main constructor.
	 */
	public WSSecurityConfigEntity(String name, boolean signing,
			KeyStoreType keyStoreType, String keyStorePath,
			String keyStorePassword, String keyEntryPassword, String alias) {

		this.name = name;
		this.signing = signing;
		this.keyStoreType = keyStoreType;
		this.keyStorePath = keyStorePath;
		this.keyStorePassword = keyStorePassword;
		this.keyEntryPassword = keyEntryPassword;
		this.alias = alias;
	}

	@Id
	public String getName() {

		return this.name;
	}

	public void setName(String name) {

		this.name = name;
	}

	public boolean isSigning() {

		return signing;
	}

	public void setSigning(boolean signing) {

		this.signing = signing;
	}

	@Enumerated(EnumType.STRING)
	public KeyStoreType getKeyStoreType() {

		return this.keyStoreType;
	}

	public void setKeyStoreType(KeyStoreType keyStoreType) {

		this.keyStoreType = keyStoreType;
	}

	public String getKeyStorePath() {

		return this.keyStorePath;
	}

	public void setKeyStorePath(String keyStorePath) {

		this.keyStorePath = keyStorePath;
	}

	public String getKeyStorePassword() {

		return this.keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {

		this.keyStorePassword = keyStorePassword;
	}

	public String getKeyEntryPassword() {

		return this.keyEntryPassword;
	}

	public void setKeyEntryPassword(String keyEntryPassword) {

		this.keyEntryPassword = keyEntryPassword;
	}

	public String getAlias() {

		return this.alias;
	}

	public void setAlias(String alias) {

		this.alias = alias;
	}
}
