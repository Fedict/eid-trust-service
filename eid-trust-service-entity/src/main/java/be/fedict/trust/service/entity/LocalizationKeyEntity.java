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
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "ts_locali_key")
public class LocalizationKeyEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private String keyName;

	private Set<LocalizationTextEntity> texts;

	/**
	 * Default constructor.
	 */
	public LocalizationKeyEntity() {
		super();

		this.texts = new HashSet<LocalizationTextEntity>();
	}

	/**
	 * Main constructor.
	 * 
	 * @param keyName
	 */
	public LocalizationKeyEntity(String keyName) {

		this.keyName = keyName;
		this.texts = new HashSet<LocalizationTextEntity>();
	}

	@Id
	public String getKeyName() {
		return this.keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	@Column(name = "texts")
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
	public Set<LocalizationTextEntity> getTexts() {

		return this.texts;
	}

	public void setTexts(Set<LocalizationTextEntity> texts) {

		this.texts = texts;
	}
}
