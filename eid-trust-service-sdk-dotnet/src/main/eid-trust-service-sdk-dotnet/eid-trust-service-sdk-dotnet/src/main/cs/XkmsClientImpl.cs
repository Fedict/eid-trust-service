using XKMS2WSNamespace;
using System;
using System.Net;
using System.Net.Security;
using System.Security.Cryptography.X509Certificates;
using System.ServiceModel;
using System.ServiceModel.Channels;
using System.ServiceModel.Description;
using System.ServiceModel.Dispatcher;
using System.ServiceModel.Security;
using System.ServiceModel.Security.Tokens;
using System.Collections.Generic;
using Org.BouncyCastle.Ocsp;
using Org.BouncyCastle.X509;
using Org.BouncyCastle.Tsp;
using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.Security;
using Org.BouncyCastle.Crypto.Parameters;
using System.Security.Cryptography;

namespace eid_trust_service_sdk_dotnet
{
    /// <summary>
    /// Client implementation of the eID Trust Service XKMS2 Web Service.
    /// </summary>
    public class XkmsClientImpl : XkmsClient
    {
        private X509Certificate2 sslCertificate;
        private X509Certificate2 serverCertificate;
        private X509Certificate2 clientCertificate;

        private String location;

        private XKMSPortTypeClient client;

        private RevocationValuesType revocationValues;
        private LinkedList<String> invalidReasonURIs = new LinkedList<String>();

        private bool CertificateValidationCallback(Object sender,
            System.Security.Cryptography.X509Certificates.X509Certificate certificate,
            X509Chain chain,
            SslPolicyErrors sslPolicyErrors)
        {
            Console.WriteLine("Certificate Validation Callback");
            bool result = certificate.Equals(this.sslCertificate);
            Console.WriteLine("TSL Authn result: " + result);
            return result;
        }

        /// <summary>
        /// Default constructor.
        /// 
        /// Set the optional authentication modes (unilateral TLS and/or WS-Security) afterwards.
        /// </summary>
        /// <param name="location">Full path of the eID Trust Service</param>
        public XkmsClientImpl(string location)
        {
            this.location = location;
        }

        public void configureSsl(X509Certificate2 sslCertificate)
        {
            this.sslCertificate = sslCertificate;
        }

        public void configureWSSecurity(X509Certificate2 serverCertificate, X509Certificate2 clientCertificate)
        {
            this.serverCertificate = serverCertificate;
            this.clientCertificate = clientCertificate;
            if (null == this.clientCertificate)
            {
                // here we generate a temporary self-signed certificate and key pair for signing the request.
                AsymmetricCipherKeyPair keyPair = KeyStoreUtil.GenerateKeyPair();
                this.clientCertificate = new X509Certificate2(
                    DotNetUtilities.ToX509Certificate(
                    KeyStoreUtil.CreateCert("CN=Temp", keyPair.Public, keyPair.Private)));
                this.clientCertificate.PrivateKey = KeyStoreUtil.getRSAPrivateKey(keyPair.Private, true);
            }
        }

        private void setupClient()
        {
            EndpointAddress remoteAddress = new EndpointAddress(this.location);

            bool sslLocation = this.location.StartsWith("https") ? true : false;
            if (sslLocation)
            {
                if (null != this.sslCertificate)
                {
                    /*
                     * Setup SSL validation
                     */
                    Console.WriteLine("SSL validation active");
                    ServicePointManager.ServerCertificateValidationCallback =
                        new RemoteCertificateValidationCallback(CertificateValidationCallback);
                }
                else
                {
                    ServicePointManager.ServerCertificateValidationCallback =
                        new RemoteCertificateValidationCallback(WCFUtil.AnyCertificateValidationCallback);
                }
            }

            if (null != this.serverCertificate)
            {
                /*
                 * Setup WS-Security
                 */
                Console.WriteLine("WS-Security active");
                this.client = new XKMSPortTypeClient(new WSSecurityBinding(sslLocation, this.serverCertificate), remoteAddress);
                // set credentials
                this.client.ClientCredentials.ServiceCertificate.DefaultCertificate = this.serverCertificate;
                this.client.ClientCredentials.ServiceCertificate.Authentication.CertificateValidationMode =
                    X509CertificateValidationMode.None;
                this.client.ClientCredentials.ClientCertificate.Certificate = this.clientCertificate;
                // set contract
                this.client.Endpoint.Contract.ProtectionLevel = ProtectionLevel.Sign;
            }

            if (null == this.client)
            {
                // Setup basic client without WS-Security binding
                if (sslLocation)
                {
                    this.client = new XKMSPortTypeClient(WCFUtil.BasicHttpOverSSLBinding(), remoteAddress);
                }
                else
                {
                    this.client = new XKMSPortTypeClient(new BasicHttpBinding(), remoteAddress);
                }
            }

            // add logging behaviour
            this.client.Endpoint.Behaviors.Add(new LoggingBehavior());
        }

        public void validate(List<Org.BouncyCastle.X509.X509Certificate> certificateChain)
        {
            validate(certificateChain, null, false, DateTime.MinValue, null, null, null, null, null);
        }

        public void validate(List<Org.BouncyCastle.X509.X509Certificate> certificateChain,
            bool returnRevocationData)
        {
            validate(certificateChain, null, returnRevocationData, DateTime.MinValue, null, null, null, null, null);
        }

        public void validate(string trustDomain, List<Org.BouncyCastle.X509.X509Certificate> certificateChain)
        {
            validate(certificateChain, trustDomain, false, DateTime.MinValue, null, null, null, null, null);
        }

        public void validate(string trustDomain, List<Org.BouncyCastle.X509.X509Certificate> certificateChain,
            bool returnRevocationData)
        {
            validate(certificateChain, trustDomain, returnRevocationData, DateTime.MinValue, null, null, null, null, null);
        }

        public void validate(string trustDomain, List<Org.BouncyCastle.X509.X509Certificate> certificateChain,
            DateTime validationDate, List<OcspResp> ocspResponses, List<X509Crl> crls)
        {
            validate(certificateChain, trustDomain, false, validationDate, ocspResponses, crls, null, null, null);
        }

        public void validate(string trustDomain, List<Org.BouncyCastle.X509.X509Certificate> certificateChain,
            DateTime validationDate, RevocationValuesType revocationValues)
        {
            validate(certificateChain, trustDomain, false, validationDate, null, null, revocationValues, null, null);
        }

        public void validate(string trustDomain, TimeStampToken timeStampToken)
        {
            validate(null, trustDomain, false, DateTime.MinValue, null, null, null, timeStampToken, null);
        }

        public void validate(string trustDomain, List<Org.BouncyCastle.X509.X509Certificate> certificateChain,
            EncapsulatedPKIDataType[] attributeCertificates)
        {
            validate(certificateChain, trustDomain, false, DateTime.MinValue, null, null, null, null, attributeCertificates);
        }

        public LinkedList<string> getInvalidReasons()
        {
            return this.invalidReasonURIs;
        }

        public RevocationValuesType getRevocationValues()
        {
            return this.revocationValues;
        }

        /*
         * Validation
         */
        private void validate(List<Org.BouncyCastle.X509.X509Certificate> certificateChain, string trustDomain,
            bool returnRevocationData, DateTime validationDate, List<OcspResp> ocspResponses, List<X509Crl> crls,
            RevocationValuesType revocationValues, TimeStampToken timeStampToken,
            EncapsulatedPKIDataType[] attributeCertificates)
        {
            // setup the client
            setupClient();

            // validate
            ValidateRequestType validateRequest = new ValidateRequestType();
            QueryKeyBindingType queryKeyBinding = new QueryKeyBindingType();
            KeyInfoType keyInfo = new KeyInfoType();
            X509DataType x509Data = new X509DataType();
            x509Data.Items = new object[certificateChain.Count];
            x509Data.ItemsElementName = new ItemsChoiceType[certificateChain.Count];
            int idx = 0;
            foreach (Org.BouncyCastle.X509.X509Certificate certificate in certificateChain)
            {
                x509Data.Items[idx] = certificate.GetEncoded();
                x509Data.ItemsElementName[idx] = ItemsChoiceType.X509Certificate;
                idx++;
            }
            keyInfo.Items = new object[] { x509Data };
            keyInfo.ItemsElementName = new ItemsChoiceType2[] { ItemsChoiceType2.X509Data };
            queryKeyBinding.KeyInfo = keyInfo;
            validateRequest.QueryKeyBinding = queryKeyBinding;

            /*
             * Set optional trust domain 
             */
            if (null != trustDomain)
            {
                UseKeyWithType useKeyWith = new UseKeyWithType();
                useKeyWith.Application = XkmsConstants.TRUST_DOMAIN_APPLICATION_URI;
                useKeyWith.Identifier = trustDomain;
                queryKeyBinding.UseKeyWith = new UseKeyWithType[] { useKeyWith };
            }

            /*
             * Add timestamp token for TSA validation
             */
            if (null != timeStampToken)
            {
                addTimeStampToken(validateRequest, timeStampToken);
            }

            /*
             * Add attribute certificates
             */
            if (null != attributeCertificates)
            {
                addAttributeCertificates(validateRequest, attributeCertificates);
            }

            /*
             * Set if used revocation data should be returned or not
             */
            if (returnRevocationData)
            {
                validateRequest.RespondWith = new string[] { XkmsConstants.RETURN_REVOCATION_DATA_URI };
            }

            /*
             * Historical validation, add the revocation data to the request
             */
            if (!validationDate.Equals(DateTime.MinValue))
            {
                TimeInstantType timeInstant = new TimeInstantType();
                timeInstant.Time = validationDate;
                queryKeyBinding.TimeInstant = timeInstant;

                addRevocationData(validateRequest, ocspResponses, crls, revocationValues);
            }

            /*
             * Validate
             */
            ValidateResultType validateResult = client.Validate(validateRequest);

            /*
             * Check result 
             */
            checkResponse(validateResult);

            /*
             * Set the optionally requested revocation data
             */
            if (returnRevocationData)
            {
                foreach (MessageExtensionAbstractType messageExtension in validateResult.MessageExtension)
                {
                    if (messageExtension is RevocationDataMessageExtensionType)
                    {
                        this.revocationValues = ((RevocationDataMessageExtensionType)messageExtension).RevocationValues;
                    }
                }
                if (null == this.revocationValues)
                {
                    throw new RevocationDataNotFoundException();
                }
            }

            /*
             * Store reason URIs
             */
            foreach (KeyBindingType keyBinding in validateResult.KeyBinding)
            {
                if (KeyBindingEnum.httpwwww3org200203xkmsValid.Equals(keyBinding.Status.StatusValue))
                {
                    return;
                }
                foreach (string reason in keyBinding.Status.InvalidReason)
                {
                    this.invalidReasonURIs.AddLast(reason);
                }
                throw new ValidationFailedException(this.invalidReasonURIs);
            }
        }

        /// <summary>
        /// Add the specified list of encoded attribute certificates to the validate request.
        /// </summary>
        /// <param name="validateRequest"></param>
        /// <param name="attributeCertificates"></param>
        private void addAttributeCertificates(ValidateRequestType validateRequest, EncapsulatedPKIDataType[] attributeCertificates)
        {
            AttributeCertificateMessageExtensionType attributeCertificateMessageExtension =
                new AttributeCertificateMessageExtensionType();
            attributeCertificateMessageExtension.CertifiedRoles = attributeCertificates;
            validateRequest.MessageExtension = new MessageExtensionAbstractType[] { attributeCertificateMessageExtension };
        }

        /// <summary>
        /// Add the specified timestamp token to the validate request.
        /// </summary>
        /// <param name="validateRequest"></param>
        /// <param name="timeStampToken"></param>
        private void addTimeStampToken(ValidateRequestType validateRequest, TimeStampToken timeStampToken)
        {
            TSAMessageExtensionType tsaMessageExtension = new TSAMessageExtensionType();
            EncapsulatedPKIDataType timeStampTokenValue = new EncapsulatedPKIDataType();
            timeStampTokenValue.Value = timeStampToken.GetEncoded();
            tsaMessageExtension.EncapsulatedTimeStamp = timeStampTokenValue;
            validateRequest.MessageExtension = new MessageExtensionAbstractType[] { tsaMessageExtension };
        }

        /// <summary>
        /// Add revocation data either from list of OCSP response objects or list of X509 CRL objects 
        /// or from specified RevocationValuesType.
        /// </summary>
        /// <param name="validateRequest"></param>
        /// <param name="ocspResponses"></param>
        /// <param name="crls"></param>
        /// <param name="revocationData"></param>
        private void addRevocationData(ValidateRequestType validateRequest, List<OcspResp> ocspResponses,
            List<X509Crl> crls, RevocationValuesType revocationData)
        {
            RevocationDataMessageExtensionType revocationDataMessageExtension =
                new RevocationDataMessageExtensionType();

            if (null != revocationData)
            {
                revocationDataMessageExtension.RevocationValues = revocationData;
            }
            else
            {
                RevocationValuesType revocationValues = new RevocationValuesType();

                // OCSP
                EncapsulatedPKIDataType[] ocspValues = new EncapsulatedPKIDataType[ocspResponses.Count];
                int idx = 0;
                foreach (OcspResp ocspResponse in ocspResponses)
                {
                    EncapsulatedPKIDataType ocspValue = new EncapsulatedPKIDataType();
                    ocspValue.Value = ocspResponse.GetEncoded();
                    ocspValues[idx++] = ocspValue;
                }
                revocationValues.OCSPValues = ocspValues;

                // CRL
                EncapsulatedPKIDataType[] crlValues = new EncapsulatedPKIDataType[crls.Count];
                idx = 0;
                foreach (X509Crl crl in crls)
                {
                    EncapsulatedPKIDataType crlValue = new EncapsulatedPKIDataType();
                    crlValue.Value = crl.GetEncoded();
                    crlValues[idx++] = crlValue;
                }
                revocationValues.CRLValues = crlValues;

                revocationDataMessageExtension.RevocationValues = revocationValues;
            }

            validateRequest.MessageExtension =
                new MessageExtensionAbstractType[] { revocationDataMessageExtension };
        }

        private void checkResponse(ValidateResultType validateResult)
        {
            Console.WriteLine("Result: " + validateResult.ResultMajor);
            if (!validateResult.ResultMajor.Equals(XkmsConstants.RESULT_MAJOR_SUCCESS))
            {
                if (validateResult.ResultMinor.Equals(XkmsConstants.RESULT_MINOR_TRUST_DOMAIN_NOT_FOUND))
                {
                    throw new TrustDomainNotFoundException();
                }
            }
        }
    }
}
