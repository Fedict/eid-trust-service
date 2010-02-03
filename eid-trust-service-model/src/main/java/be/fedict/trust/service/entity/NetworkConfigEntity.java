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

package be.fedict.trust.service.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "network_config")
public class NetworkConfigEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;

	private String proxyHost;
	private int proxyPort;

	private boolean enabled = false;

	/**
	 * Default constructor.
	 */
	public NetworkConfigEntity() {
		super();
	}

	/**
	 * Main constructor.
	 */
	public NetworkConfigEntity(String name, String proxyHost, int proxyPort) {

		this.name = name;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
	}

	@Id
	public String getName() {

		return this.name;
	}

	public void setName(String name) {

		this.name = name;
	}

	public String getProxyHost() {
		return this.proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return this.proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
