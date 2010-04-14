using System;
using System.ServiceModel;
using System.ServiceModel.Channels;
using System.ServiceModel.Security;
using System.ServiceModel.Security.Tokens;
using System.Security.Cryptography.X509Certificates;
using Org.BouncyCastle.Asn1.X509;
using System.Collections;

namespace eid_trust_service_sdk_dotnet
{
	/// <summary>
	/// WSSecurityBinding.
	/// 
	/// This custom binding provides message integrity and optional transport security using 
    /// HttpsTransportBindingElement, else HttpTransportBindingElement is used.
	/// </summary>
	public class WSSecurityBinding : Binding
	{
        private bool sslLocation;

		private BindingElementCollection bindingElements;

        public WSSecurityBinding(bool sslLocation, X509Certificate2 serviceCertificate)
        {
            this.sslLocation = sslLocation;

			// Get CN from service certificate, used to set Dns Identity Claim
            string cn = null;
            foreach (string issuerPart in serviceCertificate.Issuer.Split(','))
            {
                string[] parts = issuerPart.Split('=');
                if (parts[0].ToUpperInvariant().EndsWith("CN")) 
                {
                    cn = parts[1]; 
                }
            }

			TextMessageEncodingBindingElement encoding = new TextMessageEncodingBindingElement();
			encoding.MessageVersion = MessageVersion.Soap11;
             
			AsymmetricSecurityBindingElement securityBinding = SecurityBindingElement.CreateMutualCertificateDuplexBindingElement();
			securityBinding.LocalClientSettings.IdentityVerifier = new DnsIdentityVerifier(new DnsEndpointIdentity(cn));
			securityBinding.AllowSerializedSigningTokenOnReply = true;
			securityBinding.SecurityHeaderLayout = SecurityHeaderLayout.Lax;
             
			this.bindingElements = new BindingElementCollection();
            this.bindingElements.Add(securityBinding);
			this.bindingElements.Add(encoding);
            if (this.sslLocation)
            {
                this.bindingElements.Add(new HttpsTransportBindingElement());
            }
            else
            {
                this.bindingElements.Add(new HttpTransportBindingElement());
            }
		}
		
		public override BindingElementCollection CreateBindingElements() {
			return this.bindingElements.Clone();
		}
		
		public override string Scheme {
			get 
            {
                if (this.sslLocation)
                {
                    return "https";
                }
                else { return "http"; }
            }
		}
		
		public override IChannelFactory<TChannel> BuildChannelFactory<TChannel>(BindingParameterCollection parameters)
		{
			Console.WriteLine("build channel factory");
			return null;
		}
		
		public override bool CanBuildChannelFactory<TChannel>(BindingParameterCollection parameters)
		{
			Console.WriteLine("can build channel factory");
			return true;
		}
	}
}
