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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "ts_locali_text")
public class LocalizationTextEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private long id;

	private LocalizationKeyEntity keyName;
	private String language;
	private String text;

	/**
	 * Default constructor.
	 */
	public LocalizationTextEntity() {
		super();
	}

	/**
	 * Main constructor.
	 * 
	 * @param language
	 * @param text
	 */
	public LocalizationTextEntity(LocalizationKeyEntity keyName,
			String language, String text) {
		this.keyName = keyName;
		this.language = language;
		this.text = text;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@ManyToOne(optional = false)
	public LocalizationKeyEntity getKeyName() {
		return this.keyName;
	}

	public void setKeyName(LocalizationKeyEntity keyName) {
		this.keyName = keyName;
	}

	public String getLanguage() {
		return this.language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	@Column(length = 1024 * 1024)
	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
