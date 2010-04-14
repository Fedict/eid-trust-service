using System;
using System.Collections.Generic;
using System.Text;
using Org.BouncyCastle.Security;
using Org.BouncyCastle.Math;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Crypto;
using System.Security.Cryptography.X509Certificates;
using System.IO;
using Org.BouncyCastle.Pkcs;
using System.Security.Cryptography;
using System.Collections;
using Org.BouncyCastle.X509;
using Org.BouncyCastle.Asn1.X509;
using Org.BouncyCastle.OpenSsl;

namespace eid_trust_service_sdk_dotnet
{
    public class KeyStoreUtil
    {
        private KeyStoreUtil()
        {
        }

        public static AsymmetricCipherKeyPair GenerateKeyPair()
        {
            SecureRandom sr = new SecureRandom();
            BigInteger pubExp = new BigInteger("10001", 16);
            RsaKeyGenerationParameters RSAKeyGenPara =
               new RsaKeyGenerationParameters(pubExp, sr, 1024, 80);
            RsaKeyPairGenerator RSAKeyPairGen = new RsaKeyPairGenerator();
            RSAKeyPairGen.Init(RSAKeyGenPara);
            AsymmetricCipherKeyPair keyPair = RSAKeyPairGen.GenerateKeyPair();
            return keyPair;
        }
        public static Org.BouncyCastle.X509.X509Certificate CreateCert(String cn,
        AsymmetricKeyParameter pubKey,
        AsymmetricKeyParameter privKey)
        {
            X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

            certGen.SetSerialNumber(BigInteger.One);
            certGen.SetIssuerDN(new X509Name(cn));
            certGen.SetNotBefore(DateTime.UtcNow.AddDays(-30));
            certGen.SetNotAfter(DateTime.UtcNow.AddDays(30));
            certGen.SetSubjectDN(new X509Name(cn));
            certGen.SetPublicKey(pubKey);
            certGen.SetSignatureAlgorithm("SHA1WithRSAEncryption");

            Org.BouncyCastle.X509.X509Certificate cert = certGen.Generate(privKey);

            cert.CheckValidity(DateTime.UtcNow);

            cert.Verify(pubKey);

            return cert;
        }

        public static void WritePkcs12(RsaPrivateCrtKeyParameters privKey,
                                       Org.BouncyCastle.X509.X509Certificate certificate,
                                       string password, Stream stream)
        {
            Pkcs12Store store = new Pkcs12Store();
            X509CertificateEntry[] chain = new X509CertificateEntry[1];
            chain[0] = new X509CertificateEntry(certificate);
            store.SetKeyEntry("privateKey", new AsymmetricKeyEntry(privKey), chain);
            store.Save(stream, password.ToCharArray(), new SecureRandom());
        }

        public static RSACryptoServiceProvider GetPrivateKeyFromPfx(String pfxPath, String password, bool useMachineKeyStore)
        {

            if (useMachineKeyStore)
            {
                System.Security.Cryptography.X509Certificates.X509Certificate2 certificate =
                    new System.Security.Cryptography.X509Certificates.X509Certificate2(pfxPath, password);
                return (RSACryptoServiceProvider)certificate.PrivateKey;
            }
            else
            {
                System.Security.Cryptography.X509Certificates.X509Certificate2 certificate =
                    new System.Security.Cryptography.X509Certificates.X509Certificate2(pfxPath, password,
                                                                                       System.Security.Cryptography.X509Certificates.X509KeyStorageFlags.MachineKeySet);
                return (RSACryptoServiceProvider)certificate.PrivateKey;
            }
        }

        public static RSACryptoServiceProvider getRSAPrivateKey(AsymmetricKeyParameter privateKey, bool useMachineKeyStore)
        {
            RSAParameters keyParams = DotNetUtilities.ToRSAParameters((RsaPrivateCrtKeyParameters)privateKey);

            RSACryptoServiceProvider key;
            if (useMachineKeyStore)
            {
                CspParameters cspParameters = new CspParameters();
                cspParameters.Flags = CspProviderFlags.UseMachineKeyStore;
                key = new RSACryptoServiceProvider(cspParameters);
            }
            else
                key = new RSACryptoServiceProvider();

            key.ImportParameters(keyParams);
            return key;
        }

        public static RSACryptoServiceProvider GetPrivateKeyFromPem(String pemPath, bool useMachineKeyStore)
        {

            AsymmetricCipherKeyPair keyPair = (AsymmetricCipherKeyPair)new PemReader(new StreamReader(pemPath)).ReadObject();
            return getRSAPrivateKey(keyPair.Private, useMachineKeyStore);
        }

        public static X509Certificate2 loadCertificate(string pfxPath, string pfxPassword, bool useMachineKeyStore)
        {
            if (useMachineKeyStore)
                return new X509Certificate2(pfxPath, pfxPassword,
                                                      System.Security.Cryptography.X509Certificates.X509KeyStorageFlags.MachineKeySet);
            else
                return new X509Certificate2(pfxPath, pfxPassword);

        }
    }
}
