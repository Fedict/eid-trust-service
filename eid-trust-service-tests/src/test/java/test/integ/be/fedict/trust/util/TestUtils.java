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

package test.integ.be.fedict.trust.util;

import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAKeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.smartcardio.CardException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.joda.time.DateTime;
import org.w3c.dom.Node;

import be.fedict.eid.applet.Messages;
import be.fedict.eid.applet.Status;
import be.fedict.eid.applet.View;
import be.fedict.eid.applet.sc.PcscEid;
import be.fedict.eid.applet.sc.PcscEidSpi;

/**
 * Utility class for unit tests.
 * 
 * @author wvdhaute
 * 
 */
public class TestUtils {

	private static final Log LOG = LogFactory.getLog(TestUtils.class);

	public static final String XKMS_WS_HOST = "localhost";
	public static final String XKMS_WS_CONTEXT_PATH = "/eid-trust-service-ws/xkms2";
	public static final String XKMS_WS_LOCATION = "http://" + XKMS_WS_HOST
			+ ":8080" + XKMS_WS_CONTEXT_PATH;

	private TestUtils() {

		// empty
	}

	static {
		if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	public static KeyPair generateKeyPair() throws NoSuchAlgorithmException,
			InvalidAlgorithmParameterException {

		KeyPair keyPair = generateKeyPair("RSA");
		return keyPair;
	}

	public static KeyPair generateKeyPair(String algorithm)
			throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {

		KeyPairGenerator keyPairGenerator = KeyPairGenerator
				.getInstance(algorithm);
		SecureRandom random = new SecureRandom();
		if ("RSA".equals(keyPairGenerator.getAlgorithm())) {
			keyPairGenerator.initialize(new RSAKeyGenParameterSpec(1024,
					RSAKeyGenParameterSpec.F4), random);
		} else if (keyPairGenerator instanceof DSAKeyPairGenerator) {
			DSAKeyPairGenerator dsaKeyPairGenerator = (DSAKeyPairGenerator) keyPairGenerator;
			dsaKeyPairGenerator.initialize(512, false, random);
		}
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		return keyPair;
	}

	public static X509Certificate generateSelfSignedCertificate(
			KeyPair keyPair, String dn) throws InvalidKeyException,
			IllegalStateException, NoSuchAlgorithmException,
			SignatureException, IOException, CertificateException {

		DateTime now = new DateTime();
		DateTime notBefore = now.minusYears(10);
		DateTime future = now.plusYears(10);
		X509Certificate certificate = generateSelfSignedCertificate(keyPair,
				dn, notBefore, future, null, true, true, false);
		return certificate;
	}

	public static X509Certificate generateSelfSignedCertificate(
			KeyPair keyPair, String dn, DateTime notBefore, DateTime notAfter,
			String signatureAlgorithm, boolean includeAuthorityKeyIdentifier,
			boolean caCert, boolean timeStampingPurpose)
			throws InvalidKeyException, IllegalStateException,
			NoSuchAlgorithmException, SignatureException, IOException,
			CertificateException {

		X509Certificate certificate = generateCertificate(keyPair.getPublic(),
				dn, keyPair.getPrivate(), null, notBefore, notAfter,
				signatureAlgorithm, includeAuthorityKeyIdentifier, caCert,
				timeStampingPurpose, null);
		return certificate;
	}

	public static X509Certificate generateCertificate(
			PublicKey subjectPublicKey, String subjectDn,
			PrivateKey issuerPrivateKey, X509Certificate issuerCert,
			DateTime notBefore, DateTime notAfter, String signatureAlgorithm,
			boolean includeAuthorityKeyIdentifier, boolean caCert,
			boolean timeStampingPurpose, URI ocspUri) throws IOException,
			InvalidKeyException, IllegalStateException,
			NoSuchAlgorithmException, SignatureException, CertificateException {

		String finalSignatureAlgorithm = signatureAlgorithm;
		if (null == signatureAlgorithm) {
			finalSignatureAlgorithm = "SHA512WithRSAEncryption";
		}
		X509V3CertificateGenerator certificateGenerator = new X509V3CertificateGenerator();
		certificateGenerator.reset();
		certificateGenerator.setPublicKey(subjectPublicKey);
		certificateGenerator.setSignatureAlgorithm(finalSignatureAlgorithm);
		certificateGenerator.setNotBefore(notBefore.toDate());
		certificateGenerator.setNotAfter(notAfter.toDate());
		X509Principal issuerDN;
		if (null != issuerCert) {
			issuerDN = new X509Principal(issuerCert.getSubjectX500Principal()
					.toString());
		} else {
			issuerDN = new X509Principal(subjectDn);
		}
		certificateGenerator.setIssuerDN(issuerDN);
		certificateGenerator.setSubjectDN(new X509Principal(subjectDn));
		certificateGenerator.setSerialNumber(new BigInteger(128,
				new SecureRandom()));

		certificateGenerator.addExtension(X509Extensions.SubjectKeyIdentifier,
				false, createSubjectKeyId(subjectPublicKey));
		PublicKey issuerPublicKey;
		if (null != issuerCert) {
			issuerPublicKey = issuerCert.getPublicKey();
		} else {
			issuerPublicKey = subjectPublicKey;
		}
		if (true == includeAuthorityKeyIdentifier) {
			certificateGenerator.addExtension(
					X509Extensions.AuthorityKeyIdentifier, false,
					createAuthorityKeyId(issuerPublicKey));
		}

		certificateGenerator.addExtension(X509Extensions.BasicConstraints,
				false, new BasicConstraints(caCert));

		if (timeStampingPurpose) {
			certificateGenerator.addExtension(X509Extensions.ExtendedKeyUsage,
					true, new ExtendedKeyUsage(new DERSequence(
							KeyPurposeId.id_kp_timeStamping)));
		}

		if (null != ocspUri) {
			GeneralName ocspName = new GeneralName(
					GeneralName.uniformResourceIdentifier, ocspUri.toString());
			AuthorityInformationAccess authorityInformationAccess = new AuthorityInformationAccess(
					X509ObjectIdentifiers.ocspAccessMethod, ocspName);
			certificateGenerator.addExtension(
					X509Extensions.AuthorityInfoAccess.getId(), false,
					authorityInformationAccess);
		}

		X509Certificate certificate = certificateGenerator
				.generate(issuerPrivateKey);

		/*
		 * Make sure the default certificate provider is active.
		 */
		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		certificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(certificate
						.getEncoded()));

		return certificate;
	}

	/**
	 * Persist the given private key and corresponding certificate to a keystore
	 * file.
	 * 
	 * @param pkcs12keyStore
	 *            The file of the keystore to write the key material to.
	 * @param keyStoreType
	 *            The type of the key store format to use.
	 * @param privateKey
	 *            The private key to persist.
	 * @param certificate
	 *            The X509 certificate corresponding with the private key.
	 * @param keyStorePassword
	 *            The keystore password.
	 * @param keyEntryPassword
	 *            The keyentry password.
	 * @return
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws IOException
	 */
	public static KeyStore persistInKeyStore(File pkcs12keyStore,
			String keyStoreType, PrivateKey privateKey,
			Certificate certificate, String keyStorePassword,
			String keyEntryPassword, String alias) throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException {

		KeyStore keyStore = KeyStore.getInstance(keyStoreType);
		keyStore.load(null, keyStorePassword.toCharArray());
		keyStore.setKeyEntry(alias, privateKey, keyEntryPassword.toCharArray(),
				new Certificate[] { certificate });
		FileOutputStream keyStoreOut;
		keyStoreOut = new FileOutputStream(pkcs12keyStore);
		keyStore.store(keyStoreOut, keyStorePassword.toCharArray());
		keyStoreOut.close();

		return keyStore;
	}

	private static SubjectKeyIdentifier createSubjectKeyId(PublicKey publicKey)
			throws IOException {

		ByteArrayInputStream bais = new ByteArrayInputStream(publicKey
				.getEncoded());
		SubjectPublicKeyInfo info = new SubjectPublicKeyInfo(
				(ASN1Sequence) new ASN1InputStream(bais).readObject());
		return new SubjectKeyIdentifier(info);
	}

	private static AuthorityKeyIdentifier createAuthorityKeyId(
			PublicKey publicKey) throws IOException {

		ByteArrayInputStream bais = new ByteArrayInputStream(publicKey
				.getEncoded());
		SubjectPublicKeyInfo info = new SubjectPublicKeyInfo(
				(ASN1Sequence) new ASN1InputStream(bais).readObject());

		return new AuthorityKeyIdentifier(info);
	}

	public static List<X509Certificate> getAuthnCertificateChain()
			throws Exception, CardException, IOException, CertificateException {
		Messages messages = new Messages(Locale.getDefault());
		View view = new LogTestView(LOG);
		PcscEidSpi pcscEid = new PcscEid(view, messages);

		if (false == pcscEid.isEidPresent()) {
			LOG.debug("insert eID card...");
			pcscEid.waitForEidPresent();
		}

		List<X509Certificate> authnCertificateChain;
		try {
			authnCertificateChain = pcscEid.getAuthnCertificateChain();
		} finally {
			pcscEid.close();
		}
		return authnCertificateChain;
	}

	public static List<X509Certificate> getSignCertificateChain()
			throws Exception, CardException, IOException, CertificateException {
		Messages messages = new Messages(Locale.getDefault());
		View view = new LogTestView(LOG);
		PcscEidSpi pcscEid = new PcscEid(view, messages);

		if (false == pcscEid.isEidPresent()) {
			LOG.debug("insert eID card...");
			pcscEid.waitForEidPresent();
		}

		List<X509Certificate> signCertificateChain;
		try {
			signCertificateChain = pcscEid.getSignCertificateChain();
		} finally {
			pcscEid.close();
		}
		return signCertificateChain;
	}

	public static List<X509Certificate> getNationalRegistryCertificateChain()
			throws Exception, CardException, IOException, CertificateException {
		Messages messages = new Messages(Locale.getDefault());
		View view = new LogTestView(LOG);
		PcscEid pcscEid = new PcscEid(view, messages);

		if (false == pcscEid.isEidPresent()) {
			LOG.debug("insert eID card...");
			pcscEid.waitForEidPresent();
		}

		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");

		List<X509Certificate> nrCertificateChain = new LinkedList<X509Certificate>();
		try {
			byte[] nrCertData = pcscEid.readFile(PcscEid.RRN_CERT_FILE_ID);
			X509Certificate nrCert = (X509Certificate) certificateFactory
					.generateCertificate(new ByteArrayInputStream(nrCertData));
			nrCertificateChain.add(nrCert);
			LOG.debug("national registry certificate issuer: "
					+ nrCert.getIssuerX500Principal());
			byte[] rootCaCertData = pcscEid.readFile(PcscEid.ROOT_CERT_FILE_ID);
			X509Certificate rootCaCert = (X509Certificate) certificateFactory
					.generateCertificate(new ByteArrayInputStream(
							rootCaCertData));
			nrCertificateChain.add(rootCaCert);
		} finally {
			pcscEid.close();
		}
		return nrCertificateChain;
	}

	private static class LogTestView implements View {

		private final Log log;

		public LogTestView(Log log) {
			this.log = log;
		}

		public void addDetailMessage(String message) {
			this.log.debug(message);
		}

		public Component getParentComponent() {
			return null;
		}

		public void progressIndication(int max, int current) {
			this.log.debug("progress " + current + " of " + max);
		}

		public void setStatusMessage(Status status, String statusMessage) {
			this.log.debug(status.toString() + ": " + statusMessage);
		}

		public boolean privacyQuestion(boolean includeAddress,
				boolean includePhoto, String arg2) {

			return true;
		}
	}

	public static String domToString(Node domNode) throws TransformerException {

		Source source = new DOMSource(domNode);
		StringWriter stringWriter = new StringWriter();
		Result result = new StreamResult(stringWriter);
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(source, result);
		return stringWriter.toString();
	}

}
