using System;
using Org.BouncyCastle.X509;
using NUnit.Framework;
using eid_trust_service_sdk_dotnet;
using Org.BouncyCastle.Crypto;
using System.Collections.Generic;
using System.Security.Cryptography.X509Certificates;
using System.Security.Authentication;
using System.Net;
using System.ServiceModel.Security;

namespace eid_trust_service_sdk_dotnet.test.cs
{
    [TestFixture]
    public class TestXkmsWSSec
    {
        public static string WS_SECURITY_CERT = TestXkms.CERT_DIRECTORY_PATH + "eidtrust_wssec.crt";

        private List<Org.BouncyCastle.X509.X509Certificate> invalidCertChain;

        [SetUp]
        public void setup()
        {
            AsymmetricCipherKeyPair keyPair = KeyStoreUtil.GenerateKeyPair();
            Org.BouncyCastle.X509.X509Certificate rootCertificate = KeyStoreUtil.CreateCert(keyPair.Public, keyPair.Private);
            AsymmetricCipherKeyPair clientKeyPair = KeyStoreUtil.GenerateKeyPair();
            Org.BouncyCastle.X509.X509Certificate clientCertificate = KeyStoreUtil.CreateCert(clientKeyPair.Public, clientKeyPair.Private);
            this.invalidCertChain = new List<Org.BouncyCastle.X509.X509Certificate>();
            this.invalidCertChain.Add(rootCertificate);
            this.invalidCertChain.Add(clientCertificate);
        }

        [Test]
        public void TestInvalidChainValidWSSecuritySig()
        {
            X509Certificate2 serviceCertificate = new X509Certificate2(WS_SECURITY_CERT);
            XkmsClient client = new XkmsClientImpl(TestXkms.TRUST_SERVICE_LOCATION, serviceCertificate, null);
            try
            {
                client.validate("test", this.invalidCertChain);
                Assert.Fail();
            }
            catch (SecurityNegotiationException e)
            {
                // expected
            }
        }

    }
}
