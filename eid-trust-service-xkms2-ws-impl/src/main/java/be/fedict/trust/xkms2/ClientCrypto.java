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

package be.fedict.trust.xkms2;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.ws.security.components.crypto.Crypto;

/**
 * WSS4J Crypto implementation. This component hosts the client certificate and
 * private key as used by the WSS4J library.
 * 
 * @author wvdhaute
 * 
 */
public class ClientCrypto implements Crypto {

	private final X509Certificate certificate;

	private final PrivateKey privateKey;

	public ClientCrypto(X509Certificate certificate, PrivateKey privateKey) {

		this.certificate = certificate;
		this.privateKey = privateKey;
	}

	public String getAliasForX509Cert(Certificate cert) {

		return null;
	}

	public String getAliasForX509Cert(String issuer) {

		return null;
	}

	public String getAliasForX509Cert(byte[] subjectKeyIdentifier) {

		return null;
	}

	public String getAliasForX509Cert(String issuer, BigInteger serialNumber) {

		return null;
	}

	public String getAliasForX509CertThumb(byte[] thumb) {

		return null;
	}

	public String[] getAliasesForDN(String subjectDN) {

		return null;
	}

	public byte[] getCertificateData(boolean reverse,
			X509Certificate[] certificates) {

		return null;
	}

	public CertificateFactory getCertificateFactory() {

		return null;
	}

	public X509Certificate[] getCertificates(String alias) {

		X509Certificate[] certificates = new X509Certificate[] { this.certificate };
		return certificates;
	}

	public String getDefaultX509Alias() {

		return null;
	}

	public KeyStore getKeyStore() {

		return null;
	}

	public PrivateKey getPrivateKey(String alias, String password)
			throws Exception {

		return this.privateKey;
	}

	public byte[] getSKIBytesFromCert(X509Certificate cert) {

		return null;
	}

	public X509Certificate[] getX509Certificates(byte[] data, boolean reverse) {

		return null;
	}

	public X509Certificate loadCertificate(InputStream inputStream) {

		return null;
	}

	public boolean validateCertPath(X509Certificate[] certificates) {

		return false;
	}

}
