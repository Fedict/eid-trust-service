package be.fedict.trust.client;

import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SSLTrustManager implements X509TrustManager {

	private static final Log LOG = LogFactory.getLog(SSLTrustManager.class);

	private SSLTrustManager() {

		// empty
	}

	private static SSLSocketFactory socketFactory;
	private static PublicKey trustedPublicKey;

	public static synchronized void initialize() {

		LOG.debug("initialize");
		if (null == socketFactory) {

			initSocketFactory();
			HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory);
		} else {
			if (false == socketFactory.equals(HttpsURLConnection
					.getDefaultSSLSocketFactory()))
				throw new RuntimeException("wrong SSL socket factory installed");
		}
	}

	public static synchronized void reset() {
		socketFactory = null;
		trustedPublicKey = null;
	}

	/**
	 * Sets the trusted public key to be used by this trust manager during SSL
	 * handshake for expressing trust towards the service.
	 * 
	 * @param trustedPublicKey
	 */
	public static void setTrustedPublicKey(PublicKey trustedPublicKey) {

		SSLTrustManager.trustedPublicKey = trustedPublicKey;
	}

	private static void initSocketFactory() {

		LOG.debug("init socket factory");
		SSLTrustManager trustManagerInstance = new SSLTrustManager();
		TrustManager[] trustManager = { trustManagerInstance };
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			SecureRandom secureRandom = new SecureRandom();
			sslContext.init(null, trustManager, secureRandom);
			LOG.debug("SSL context provider: "
					+ sslContext.getProvider().getName());
			socketFactory = sslContext.getSocketFactory();
		} catch (KeyManagementException e) {
			String msg = "key management error: " + e.getMessage();
			LOG.error(msg, e);
			throw new RuntimeException(msg, e);
		} catch (NoSuchAlgorithmException e) {
			String msg = "TLS algo not present: " + e.getMessage();
			LOG.error(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {

		X509Certificate serverCertificate = chain[0];
		LOG.debug("server X509 subject: "
				+ serverCertificate.getSubjectX500Principal().toString());
		LOG.debug("authentication type: " + authType);
		if (null == SSLTrustManager.trustedPublicKey)
			return;

		try {
			serverCertificate.verify(SSLTrustManager.trustedPublicKey);
			LOG.debug("valid server certificate");
		} catch (InvalidKeyException e) {
			throw new CertificateException("Invalid Key");
		} catch (NoSuchAlgorithmException e) {
			throw new CertificateException("No such algorithm");
		} catch (NoSuchProviderException e) {
			throw new CertificateException("No such provider");
		} catch (SignatureException e) {
			throw new CertificateException("Wrong signature");
		}

	}

	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {

		throw new CertificateException(
				"this trust manager cannot be used as server-side trust manager");
	}

	public X509Certificate[] getAcceptedIssuers() {

		return null;
	}

}
