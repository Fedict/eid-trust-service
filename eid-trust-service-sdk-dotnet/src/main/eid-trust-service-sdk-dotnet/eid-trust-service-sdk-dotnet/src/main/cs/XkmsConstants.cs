using System;
using System.Collections.Generic;
using System.Text;

namespace eid_trust_service_sdk_dotnet
{
    public class XkmsConstants
    {
        private XkmsConstants()
        {
        }

        public static string TRUST_DOMAIN_APPLICATION_URI = "urn:be:fedict:trust:trust-domain";
        public static string RETURN_REVOCATION_DATA_URI = "urn:be:fedict:trust:revocation-data";

        public static string KEY_BINDING_STATUS_REASON_ISSUER_TRUST_URI = "http://www.w3.org/2002/03/xkms#IssuerTrust";
        public static string KEY_BINDING_STATUS_REASON_REVOCATION_STATUS_URI = "http://www.w3.org/2002/03/xkms#RevocationStatus";
        public static string KEY_BINDING_STATUS_REASON_VALIDITY_INTERVAL_URI = "http://www.w3.org/2002/03/xkms#ValidityInterval";
        public static string KEY_BINDING_STATUS_REASON_SIGNATURE_URI = "http://www.w3.org/2002/03/xkms#Signature";

        public static string RESULT_MAJOR_SUCCESS = "http://www.w3.org/2002/03/xkms#Success";
        public static string RESULT_MAJOR_VERSION_MISMATCH = "http://www.w3.org/2002/03/xkms#VersionMismatch";
        public static string RESULT_MAJOR_SENDER = "http://www.w3.org/2002/03/xkms#Sender";
        public static string RESULT_MAJOR_RECEIVER = "http://www.w3.org/2002/03/xkms#Receiver";
        public static string RESULT_MAJOR_REPRESENT = "http://www.w3.org/2002/03/xkms#Represent";
        public static string RESULT_MAJOR_PENDING = "http://www.w3.org/2002/03/xkms#Pending";

        public static string RESULT_MINOR_NO_MATCH = "http://www.w3.org/2002/03/xkms#NoMatch";
        public static string RESULT_MINOR_TOO_MANY_RESPONSES = "http://www.w3.org/2002/03/xkms#TooManyResponses";
        public static string RESULT_MINOR_INCOMPLETE = "http://www.w3.org/2002/03/xkms#Incomplete";
        public static string RESULT_MINOR_FAILURE = "http://www.w3.org/2002/03/xkms#Failure";
        public static string RESULT_MINOR_REFUSED = "http://www.w3.org/2002/03/xkms#Refused";
        public static string RESULT_MINOR_NO_AUTHENTICATION = "http://www.w3.org/2002/03/xkms#NoAuthentication";
        public static string RESULT_MINOR_MESSAGE_NOT_SUPPORTED = "http://www.w3.org/2002/03/xkms#MessageNotSupported";
        public static string RESULT_MINOR_UNKNOWN_RESPONSE_ID = "http://www.w3.org/2002/03/xkms#UnknownResponseId";
        public static string RESULT_MINOR_REPRESENT_REQUIRED = "http://www.w3.org/2002/03/xkms#RepresentRequired";
        public static string RESULT_MINOR_NOT_SYNCHRONOUS = "http://www.w3.org/2002/03/xkms#NotSynchronous";
        public static string RESULT_MINOR_OPTIONAL_ELEMENT_NOT_SUPPORTED = "http://www.w3.org/2002/03/xkms#OptionalElementNotSupported";
        public static string RESULT_MINOR_PROOF_OF_POSESSION_REQUIRED = "http://www.w3.org/2002/03/xkms#ProofOfPossessionRequired";
        public static string RESULT_MINOR_TIME_INSTANT_NOT_SUPPORTED = "http://www.w3.org/2002/03/xkms#TimeInstantNotSupported";
        public static string RESULT_MINOR_TIME_INSTANT_OUT_OF_RANGE = "http://www.w3.org/2002/03/xkms#TimeInstantOutOfRange";
        public static string RESULT_MINOR_TRUST_DOMAIN_NOT_FOUND = "urn:be:fedict:trust:TrustDomainNotFound";
    }
}
