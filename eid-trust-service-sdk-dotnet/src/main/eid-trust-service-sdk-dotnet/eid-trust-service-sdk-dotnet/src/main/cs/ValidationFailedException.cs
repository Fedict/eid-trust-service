using System;
using System.Collections.Generic;
using System.Text;

namespace eid_trust_service_sdk_dotnet
{
    public class ValidationFailedException : Exception
    {
        private LinkedList<String> reasonURIs;

        public ValidationFailedException(LinkedList<String> reasonURIs)
        {
            this.reasonURIs = reasonURIs;
        }

        /// <summary>
        /// Returns the XKMS v2.0 reason URIs for the failed validation
        /// <see cref="http://www.w3.org/TR/xkms2/#XKMS_2_0_Section_5_1"/>
        /// </summary>
        /// <returns></returns>
        public LinkedList<String> getReasonURIs()
        {
            return this.reasonURIs;
        }
    }
}
