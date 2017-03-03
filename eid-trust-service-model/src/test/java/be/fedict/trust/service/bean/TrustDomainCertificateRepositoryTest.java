package be.fedict.trust.service.bean;

import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;

@RunWith(MockitoJUnitRunner.class)
public class TrustDomainCertificateRepositoryTest {

	private static final String TRUST_DOMAIN_CERTIFICATE_NAME = "trustDomainCertificateName";
	private static final String DIFFERENT_NAME = "differentName";

	private static final int TRUST_DOMAIN_CERTIFICATE_PUBLIC_KEY = 1;
	private static final int DIFFERENT_PUBLIC_KEY = 2;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private TrustPointEntity trustPoint;

	@Mock
	private TrustDomainEntity trustDomain;

	@Mock
	private X509Certificate trustDomainCertificate, certificateToCheck;

	private TrustDomainCertificateRepository trustDomainCertificateRepository;

	@Before
	public void setUp() {
		when(trustDomain.getTrustPoints()).thenReturn(singletonList(trustPoint));

		when(trustPoint.getCertificateAuthority().getCertificate()).thenReturn(trustDomainCertificate);
		when(trustDomainCertificate.getSubjectDN()).thenReturn(new MockPrincipal(TRUST_DOMAIN_CERTIFICATE_NAME));
		when(trustDomainCertificate.getPublicKey()).thenReturn(new MockPublicKey(TRUST_DOMAIN_CERTIFICATE_PUBLIC_KEY));

		trustDomainCertificateRepository = new TrustDomainCertificateRepository(trustDomain);
	}

	@Test
	public void correctlyMatchesWithTrustPointWithSameSubjectAndPublicKey() {
		when(certificateToCheck.getSubjectDN()).thenReturn(new MockPrincipal(TRUST_DOMAIN_CERTIFICATE_NAME));
		when(certificateToCheck.getPublicKey()).thenReturn(new MockPublicKey(TRUST_DOMAIN_CERTIFICATE_PUBLIC_KEY));

		assertTrue(trustDomainCertificateRepository.isTrustPoint(certificateToCheck));
	}

	@Test
	public void doesNotMatchWithTrustPointWithSameSubjectAndDifferentPublicKey() {
		when(certificateToCheck.getSubjectDN()).thenReturn(new MockPrincipal(TRUST_DOMAIN_CERTIFICATE_NAME));
		when(certificateToCheck.getPublicKey()).thenReturn(new MockPublicKey(DIFFERENT_PUBLIC_KEY));

		assertFalse(trustDomainCertificateRepository.isTrustPoint(certificateToCheck));
	}

	@Test
	public void doesNotMatchWithTrustPointWithDifferentSubjectAndSamePublicKey() {
		when(certificateToCheck.getSubjectDN()).thenReturn(new MockPrincipal(DIFFERENT_NAME));
		when(certificateToCheck.getPublicKey()).thenReturn(new MockPublicKey(TRUST_DOMAIN_CERTIFICATE_PUBLIC_KEY));

		assertFalse(trustDomainCertificateRepository.isTrustPoint(certificateToCheck));
	}

	private static class MockPublicKey implements PublicKey {

		private int value;

		public MockPublicKey(int value) {
			this.value = value;
		}

		@Override
		public String getAlgorithm() {
			return null;
		}

		@Override
		public String getFormat() {
			return null;
		}

		@Override
		public byte[] getEncoded() {
			return new byte[0];
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof MockPublicKey) && ((MockPublicKey)o).value == value;
		}
	}

	private static class MockPrincipal implements  Principal {

		private String name;

		private MockPrincipal(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof MockPrincipal) && ((MockPrincipal)o).name.equals(name);
		}
	}
}