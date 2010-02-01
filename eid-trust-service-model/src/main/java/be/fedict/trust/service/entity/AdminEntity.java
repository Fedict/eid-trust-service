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
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "admin")
public class AdminEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;
	private String name;
	private byte[] encodedPublicKey;

	/**
	 * Default constructor.
	 */
	public AdminEntity() {
		super();
	}

	/**
	 * Main constructor.
	 * 
	 * @param publicKey
	 */
	public AdminEntity(String id, String name, PublicKey publicKey) {
		this.id = id;
		this.name = name;
		this.encodedPublicKey = publicKey.getEncoded();
	}

	@Id
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Lob
	@Basic(fetch = FetchType.LAZY)
	public byte[] getEncodedPublicKey() {
		return this.encodedPublicKey;
	}

	public void setEncodedPublicKey(byte[] encodedPublicKey) {
		this.encodedPublicKey = encodedPublicKey;
	}

	@Transient
	public PublicKey getPublicKey() {

		try {
			X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(
					this.encodedPublicKey);
			boolean isSupportedKey = false;
			KeyFactory factory;
			PublicKey retKey = null;

			try {
				factory = KeyFactory.getInstance("RSA");
				retKey = factory.generatePublic(pubSpec);
				isSupportedKey = true;
			} catch (InvalidKeySpecException e) {
			}

			// if not RSA
			if (!isSupportedKey) {
				throw new RuntimeException("Unsupported key spec: Not RSA");
			}

			return retKey;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
