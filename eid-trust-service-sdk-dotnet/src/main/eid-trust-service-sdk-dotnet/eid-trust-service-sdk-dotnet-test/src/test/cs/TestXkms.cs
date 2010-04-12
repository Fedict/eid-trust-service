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
    public class TestXkms
    {
        public static string TRUST_SERVICE_LOCATION = "https://sebeco-dev-11:8443";
        public static string CERT_DIRECTORY_PATH = "C:\\Users\\devel\\certificates\\";
        public static string SSL_CERT_PATH = CERT_DIRECTORY_PATH + "eidtrust_ssl.cer";
        public static string INVALID_SSL_CERT_PATH = CERT_DIRECTORY_PATH + "invalid_ssl.cer";
        public static string TEST_TRUST_DOMAIN = "test";

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
        public void TestInvalidChain()
        {

            XkmsClient client = new XkmsClientImpl(TRUST_SERVICE_LOCATION);
            client.validate(TEST_TRUST_DOMAIN, this.invalidCertChain);
        }

        [Test]
        public void TestInvalidTrustDomain()
        {

            XkmsClient client = new XkmsClientImpl(TRUST_SERVICE_LOCATION);
            try
            {
                client.validate("f00", this.invalidCertChain);
                Assert.Fail();
            }
            catch (TrustDomainNotFoundException e)
            {
                // expected
            }
        }

        [Test]
        public void TestInvalidChainValidTslAuthn()
        {
            X509Certificate2 sslCertificate = new X509Certificate2(SSL_CERT_PATH);
            XkmsClient client = new XkmsClientImpl(TRUST_SERVICE_LOCATION, null, sslCertificate);
            client.validate(TEST_TRUST_DOMAIN, this.invalidCertChain);
        }

        [Test]
        public void TestInvalidChainInvalidTslAuthn()
        {
            X509Certificate2 invalidSslCertificate = new X509Certificate2(INVALID_SSL_CERT_PATH);
            XkmsClient client = new XkmsClientImpl(TRUST_SERVICE_LOCATION, null, invalidSslCertificate);
            try
            {
                client.validate(TEST_TRUST_DOMAIN, this.invalidCertChain); 
                Assert.Fail();
            }
            catch (SecurityNegotiationException e)
            {
                // expected
            }
        }
    }
}
