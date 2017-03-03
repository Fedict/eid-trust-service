package test.integ.be.fedict.client;

import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import be.fedict.trust.client.XKMS2Client;

public class TrustClientTest {

	private XKMS2Client xkms2Client;

	@Before
	public void setUp() throws Exception {
		xkms2Client = new XKMS2Client("http://localhost:8080/eid-trust-service-ws/xkms2");
	}

	@Test
	public void validatesWithFirstRoot() throws Exception {
		List<X509Certificate> certificates = Arrays.asList(
				readCertificate("/test-certificates/test-root1.crt"),
				readCertificate("/test-certificates/root1.crt")
		);

		xkms2Client.validate(certificates);
	}

	@Test
	public void validatesWithSecondRoot() throws Exception {
		List<X509Certificate> certificates = Arrays.asList(
				readCertificate("/test-certificates/test-root2.crt"),
				readCertificate("/test-certificates/root2.crt")
		);

		xkms2Client.validate(certificates);
	}

	private X509Certificate readCertificate(String resourceName) throws CertificateException {
		InputStream inputStream = TrustClientTest.class.getResourceAsStream(resourceName);
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		return (X509Certificate) factory.generateCertificate(inputStream);
	}

}
