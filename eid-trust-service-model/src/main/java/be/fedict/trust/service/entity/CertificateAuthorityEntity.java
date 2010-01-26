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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

@Entity
@Table(name = "certificate_authorities")
public class CertificateAuthorityEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public CertificateAuthorityEntity() {
		super();
	}

	/**
	 * Main constructor.
	 * 
	 * @param name
	 * @param crlUrl
	 * @param certificate
	 * @throws CertificateEncodingException
	 */
	public CertificateAuthorityEntity(String name, String crlUrl,
			X509Certificate certificate, TrustDomainEntity trustDomain)
			throws CertificateEncodingException {
		this.crlUrl = crlUrl;
		this.name = name;
		this.status = Status.INACTIVE;
		this.encodedCertificate = certificate.getEncoded();
		this.thisUpdate = null;
		this.nextUpdate = null;
		this.trustDomain = trustDomain;
	}

	private String name;

	private String crlUrl;

	private Status status;

	private byte[] encodedCertificate;

	private Date thisUpdate;

	private Date nextUpdate;

	private TrustDomainEntity trustDomain;

	@Id
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCrlUrl() {
		return this.crlUrl;
	}

	public void setCrlUrl(String crlUrl) {
		this.crlUrl = crlUrl;
	}

	@Enumerated(EnumType.STRING)
	public Status getStatus() {
		return this.status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@Lob
	@Basic(fetch = FetchType.LAZY)
	public byte[] getEncodedCertificate() {
		return this.encodedCertificate;
	}

	public void setEncodedCertificate(byte[] encodedCertificate) {
		this.encodedCertificate = encodedCertificate;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getThisUpdate() {
		return this.thisUpdate;
	}

	public void setThisUpdate(Date thisUpdate) {
		this.thisUpdate = thisUpdate;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getNextUpdate() {
		return this.nextUpdate;
	}

	public void setNextUpdate(Date nextUpdate) {
		this.nextUpdate = nextUpdate;
	}

	@ManyToOne(optional = false)
	public TrustDomainEntity getTrustDomain() {

		return trustDomain;
	}

	public void setTrustDomain(TrustDomainEntity trustDomain) {

		this.trustDomain = trustDomain;
	}

	@Transient
	public X509Certificate getCertificate() throws CertificateException {
		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		InputStream certificateStream = new ByteArrayInputStream(
				this.encodedCertificate);
		X509Certificate certificate = (X509Certificate) certificateFactory
				.generateCertificate(certificateStream);
		return certificate;
	}
}
