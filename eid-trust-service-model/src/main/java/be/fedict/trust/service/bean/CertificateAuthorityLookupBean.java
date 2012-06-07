/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2012 FedICT.
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;

import be.fedict.trust.service.dao.CertificateAuthorityDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class CertificateAuthorityLookupBean {

	private static final Log LOG = LogFactory
			.getLog(CertificateAuthorityLookupBean.class);

	@EJB
	private CertificateAuthorityDAO certificateAuthorityDAO;

	private Map<String, String> lookupMap;

	public CertificateAuthorityEntity lookup(byte[] caNameHash, byte[] caKeyHash) {
		String caNameHashKey = Hex.encodeHexString(caNameHash);
		String caName = null;
		if (null != this.lookupMap) {
			caName = this.lookupMap.get(caNameHashKey);
		}
		if (null == caName) {
			try {
				refreshLookupMap();
			} catch (Exception e) {
				LOG.error("error refreshing the lookup map: " + e.getMessage(),
						e);
				this.lookupMap = new HashMap<String, String>();
			}
		}
		caName = this.lookupMap.get(caNameHashKey);
		if (null == caName) {
			LOG.warn("unknown CA");
			return null;
		}
		CertificateAuthorityEntity caEntity = this.certificateAuthorityDAO
				.findCertificateAuthority(caName);
		return caEntity;
	}

	private void refreshLookupMap() throws CertificateEncodingException,
			NoSuchAlgorithmException {
		LOG.debug("refreshing lookup map");
		List<CertificateAuthorityEntity> activeCertificateAuthorities = this.certificateAuthorityDAO
				.listActiveCertificateAuthorities();
		Map<String, String> freshLookupMap = new HashMap<String, String>();
		for (CertificateAuthorityEntity certificateAuthority : activeCertificateAuthorities) {
			X509Certificate caCert = certificateAuthority.getCertificate();
			X509Principal issuerName = PrincipalUtil
					.getSubjectX509Principal(caCert);
			byte[] issuerNameHash = getHash(issuerName.getEncoded());
			String caNameHashKey = Hex.encodeHexString(issuerNameHash);
			String caName = caCert.getSubjectX500Principal().toString();
			freshLookupMap.put(caNameHashKey, caName);
			LOG.debug("lookup entry: " + caNameHashKey + " = " + caName);

		}
		this.lookupMap = freshLookupMap; // concurrency is no problem here
	}

	private byte[] getHash(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
		byte[] hash = messageDigest.digest(data);
		return hash;
	}
}
