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

package be.fedict.trust.client;

import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.callback.CallbackHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoType;

/**
 * WSS4J Crypto implementation. This component is used by wss4j during
 * validation of the incoming WS-Security header when reading in the embedded
 * {@link X509Certificate}
 * 
 * @author wvdhaute
 * @author Frank Cornelis
 */
public class ServerCrypto implements Crypto {

	private static final Log LOG = LogFactory.getLog(ServerCrypto.class);

	private X509Certificate certificate;

	public byte[] getBytesFromCertificates(X509Certificate[] certs)
			throws WSSecurityException {
		LOG.debug("getBytesFromCertificates");
		return null;
	}

	public CertificateFactory getCertificateFactory()
			throws WSSecurityException {
		LOG.debug("getCertificateFactory");
		return null;
	}

	public X509Certificate[] getCertificatesFromBytes(byte[] data)
			throws WSSecurityException {
		LOG.debug("getCertificatesFromBytes");
		return null;
	}

	public String getCryptoProvider() {
		LOG.debug("getCryptoProvider");
		return null;
	}

	public String getDefaultX509Identifier() throws WSSecurityException {
		LOG.debug("getDefaultX509Identifier");
		return null;
	}

	public PrivateKey getPrivateKey(X509Certificate certificate,
			CallbackHandler callbackHandler) throws WSSecurityException {
		LOG.debug("getPrivateKey(cert, callback)");
		return null;
	}

	public PrivateKey getPrivateKey(String identifier, String password)
			throws WSSecurityException {
		LOG.debug("getPrivateKey(identifier, password)");
		return null;
	}

	public byte[] getSKIBytesFromCert(X509Certificate cert)
			throws WSSecurityException {
		LOG.debug("getSKIBytesFromCert");
		return null;
	}

	public X509Certificate[] getX509Certificates(CryptoType cryptoType)
			throws WSSecurityException {
		LOG.debug("getX509Certificates: " + cryptoType.getSubjectDN());
		// trusting everything here... not to be used for production
		return new X509Certificate[] { this.certificate };
	}

	public String getX509Identifier(X509Certificate cert)
			throws WSSecurityException {
		LOG.debug("getX509Identifier");
		return null;
	}

	public X509Certificate loadCertificate(InputStream in)
			throws WSSecurityException {
		LOG.debug("loadCertificate");
		CertificateFactory certificateFactory;
		try {
			certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			throw new WSSecurityException("X509 algo", e);
		}
		X509Certificate certificate;
		try {
			certificate = (X509Certificate) certificateFactory
					.generateCertificate(in);
		} catch (CertificateException e) {
			throw new WSSecurityException("X509 error: " + e.getMessage(), e);
		}
		if (null == this.certificate) {
			LOG.debug("trusting incoming certificate as is");
			this.certificate = certificate;
		}
		return certificate;
	}

	public void setCertificateFactory(String provider,
			CertificateFactory certFactory) {
		LOG.debug("setCertifiateFactory");
	}

	public void setCryptoProvider(String provider) {
		LOG.debug("setCryptoProvider");
	}

	public void setDefaultX509Identifier(String identifier) {
		LOG.debug("setDefaultX509Identifier");
	}

	public boolean verifyTrust(X509Certificate[] certs)
			throws WSSecurityException {
		LOG.debug("verifyTrust(certs)");
		return false;
	}

	public boolean verifyTrust(PublicKey publicKey) throws WSSecurityException {
		LOG.debug("verifyTrust(publicKey)");
		return false;
	}

	public boolean verifyTrust(X509Certificate[] certs, boolean enableRevocation)
			throws WSSecurityException {
		LOG.debug("verifyTrust(certs, enableRevocation)");
		return false;
	}
}
