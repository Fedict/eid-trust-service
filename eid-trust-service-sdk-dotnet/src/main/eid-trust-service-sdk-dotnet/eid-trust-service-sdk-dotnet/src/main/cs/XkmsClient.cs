using System;
using System.Collections.Generic;
using System.Security.Cryptography.X509Certificates;
using Org.BouncyCastle.Ocsp;
using Org.BouncyCastle.X509;
using XKMS2WSNamespace;
using Org.BouncyCastle.Tsp;

namespace eid_trust_service_sdk_dotnet
{
    public interface XkmsClient
    {
        /// <summary>
        /// Set the certificate to validate the SSL certificate against. 
        /// If not specified, can still run the client over SSL just no validation of it will occur.
        /// </summary>
        /// <param name="sslCertificate"></param>
        void configureSsl(X509Certificate2 sslCertificate);

        /// <summary>
        /// Set the server certificate for WS-Security validation.
        /// This requires the eID Trust Service connecting to, to have WS-Security signing enabled.
        /// If no client certificate is specified, a temporary self-signed will be created to sign
        /// the outgoing validation request.
        /// <param name="serverCertificate">Server certificate for validation of the incoming signed response</param>
        /// <param name="clientCertificate">Optional client certificate. If specified has to include the
        /// private key for signing the outgoing request.</param>
        /// </summary>
        void configureWSSecurity(X509Certificate2 serverCertificate, X509Certificate2 clientCertificate);

        /// <summary>
        /// Validate the specified certificate chain against the default trust domain configured
        /// at the eID Trust Service we are connecting to.
        /// </summary>
        /// <param name="certificateChain"></param>
        /// <exception cref="TrustDomainNotFoundException">Case no default trust domain is set</exception>
        /// <exception cref="RevocationDataNotFoundException">Revocation data expected to be returned is missing</exception>
        /// <exception cref="ValidationFailedException">Validation failed, the exception contains the XKMS reason URIs</exception>
        void validate(List<Org.BouncyCastle.X509.X509Certificate> certificateChain);

        /// <summary>
        /// Validate the specified certificate chain against the default trust domain configured
        /// at the eID Trust Service we are connecting to.
        /// </summary>
        /// <param name="certificateChain"></param>
        /// <param name="returnRevocationData">if true, the used revocation data will be returned.</param>
        /// <exception cref="TrustDomainNotFoundException">Case no default trust domain is set</exception>
        /// <exception cref="RevocationDataNotFoundException">Revocation data expected to be returned is missing</exception>
        /// <exception cref="ValidationFailedException">Validation failed, the exception contains the XKMS reason URIs</exception>
        void validate(List<Org.BouncyCastle.X509.X509Certificate> certificateChain, bool returnRevocationData);

        /// <summary>
        /// Validate the specified certificate chain against the specified trust domain.
        /// </summary>
        /// <param name="trustDomain"></param>
        /// <param name="certificateChain"></param>
        /// <exception cref="TrustDomainNotFoundException">Case the specified trust domain was not found</exception>
        /// <exception cref="RevocationDataNotFoundException">Revocation data expected to be returned is missing</exception>
        /// <exception cref="ValidationFailedException">Validation failed, the exception contains the XKMS reason URIs</exception>
        void validate(string trustDomain, List<Org.BouncyCastle.X509.X509Certificate> certificateChain);

        /// <summary>
        /// Validate the specified certificate chain against the specified trust domain.
        /// </summary>
        /// <param name="trustDomain"></param>
        /// <param name="certificateChain"></param>
        /// <param name="returnRevocationData">if true, the used revocation data will be returned.</param>
        /// <exception cref="TrustDomainNotFoundException">Case the specified trust domain was not found</exception>
        /// <exception cref="RevocationDataNotFoundException">Revocation data expected to be returned is missing</exception>
        /// <exception cref="ValidationFailedException">Validation failed, the exception contains the XKMS reason URIs</exception>
        void validate(string trustDomain, List<Org.BouncyCastle.X509.X509Certificate> certificateChain, bool returnRevocationData);

        /// <summary>
        /// Validate the specified certificate chain against the specified trust domain using historical
        /// validation using the specified revocation data
        /// </summary>
        /// <param name="trustDomain"></param>
        /// <param name="certificateChain"></param>
        /// <param name="validationDate"></param>
        /// <param name="ocspResponses"></param>
        /// <param name="crls"></param>
        /// <exception cref="TrustDomainNotFoundException">Case the specified trust domain was not found</exception>
        /// <exception cref="RevocationDataNotFoundException">Revocation data expected to be returned is missing</exception>
        /// <exception cref="ValidationFailedException">Validation failed, the exception contains the XKMS reason URIs</exception>
        void validate(string trustDomain, List<Org.BouncyCastle.X509.X509Certificate> certificateChain, DateTime validationDate,
            List<OcspResp> ocspResponses, List<X509Crl> crls);

        /// <summary>
        /// Validate the specified certificate chain against the specified trust domain using historical
        /// validation using the specified revocation data
        /// </summary>
        /// <param name="trustDomain"></param>
        /// <param name="certificateChain"></param>
        /// <param name="validationDate"></param>
        /// <param name="revocationValues"></param>
        /// <exception cref="TrustDomainNotFoundException">Case the specified trust domain was not found</exception>
        /// <exception cref="RevocationDataNotFoundException">Revocation data expected to be returned is missing</exception>
        /// <exception cref="ValidationFailedException">Validation failed, the exception contains the XKMS reason URIs</exception>
        void validate(string trustDomain, List<Org.BouncyCastle.X509.X509Certificate> certificateChain, DateTime validationDate,
            RevocationValuesType revocationValues);

        /// <summary>
        /// Validate the specified timestamp token for the specified TSA trust domain.
        /// </summary>
        /// <param name="trustDomain"></param>
        /// <param name="timeStampToken"></param>
        /// <exception cref="TrustDomainNotFoundException">Case the specified trust domain was not found</exception>
        /// <exception cref="RevocationDataNotFoundException">Revocation data expected to be returned is missing</exception>
        /// <exception cref="ValidationFailedException">Validation failed, the exception contains the XKMS reason URIs</exception>
        void validate(string trustDomain, TimeStampToken timeStampToken);

        /// <summary>
        /// Validate the specified encoded attribute certificates and their certificate chain agains the specified trust domain.
        /// </summary>
        /// <param name="trustDomain"></param>
        /// <param name="certificateChain"></param>
        /// <param name="attributeCertificates"></param>
        /// <exception cref="TrustDomainNotFoundException">Case the specified trust domain was not found</exception>
        /// <exception cref="RevocationDataNotFoundException">Revocation data expected to be returned is missing</exception>
        /// <exception cref="ValidationFailedException">Validation failed, the exception contains the XKMS reason URIs</exception>
        void validate(string trustDomain, List<Org.BouncyCastle.X509.X509Certificate> certificateChain,
            EncapsulatedPKIDataType[] attributeCertificates);

        /// <summary>
        /// Returns the XKMS v2.0 reason URIs for the failed validation.
        /// <see cref="http://www.w3.org/TR/xkms2/#XKMS_2_0_Section_5_1"/>
        /// </summary>
        LinkedList<String> getInvalidReasons();

        /// <summary>
        /// Returns the optionally filled in revocation values if requested, else returns null. 
        /// </summary
        RevocationValuesType getRevocationValues();
    }
}
