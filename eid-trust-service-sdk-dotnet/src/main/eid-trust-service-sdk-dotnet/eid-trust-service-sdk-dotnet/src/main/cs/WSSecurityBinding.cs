using System;
using System.ServiceModel;
using System.ServiceModel.Channels;
using System.ServiceModel.Security;
using System.ServiceModel.Security.Tokens;
using System.Security.Cryptography.X509Certificates;

namespace eid_trust_service_sdk_dotnet
{
	/// <summary>
	/// WSSecurityBinding.
	/// 
	/// This custom binding provides both transport security as message integrity.
	/// </summary>
	public class WSSecurityBinding : Binding
	{
		private BindingElementCollection bindingElements;

        public WSSecurityBinding(X509Certificate2 serviceCertificate)
        {
			
			// Get CN from service certificate, used to set Dns Identity Claim
			string[] issuer = serviceCertificate.Issuer.Split(',');
			string cn = issuer[0].Split('=')[1];

			HttpsTransportBindingElement httpsTransport = new HttpsTransportBindingElement();
			TextMessageEncodingBindingElement encoding = new TextMessageEncodingBindingElement();
			encoding.MessageVersion = MessageVersion.Soap11;

            //AsymmetricSecurityBindingElement securityBinding = new AsymmetricSecurityBindingElement(new X509SecurityTokenParameters(X509KeyIdentifierClauseType.Any, SecurityTokenInclusionMode.AlwaysToInitiator, false));
            //securityBinding.MessageSecurityVersion = MessageSecurityVersion.WSSecurity11WSTrustFebruary2005WSSecureConversationFebruary2005WSSecurityPolicy11;

            
            SymmetricSecurityBindingElement securityBinding = SecurityBindingElement.CreateAnonymousForCertificateBindingElement();
            securityBinding.LocalClientSettings.IdentityVerifier = new DnsIdentityVerifier(new DnsEndpointIdentity(cn));
            securityBinding.SecurityHeaderLayout = SecurityHeaderLayout.Lax;
           

            /*
            SymmetricSecurityBindingElement securityBinding = new SymmetricSecurityBindingElement(new X509SecurityTokenParameters(X509KeyIdentifierClauseType.Thumbprint, SecurityTokenInclusionMode.Never));
            securityBinding.MessageSecurityVersion = MessageSecurityVersion.WSSecurity11WSTrustFebruary2005WSSecureConversationFebruary2005WSSecurityPolicy11;
            securityBinding.RequireSignatureConfirmation = true;
            */
             
            /*
			AsymmetricSecurityBindingElement securityBinding = SecurityBindingElement.CreateMutualCertificateDuplexBindingElement();
			securityBinding.LocalClientSettings.IdentityVerifier = new DnsIdentityVerifier(new DnsEndpointIdentity(cn));
			securityBinding.AllowSerializedSigningTokenOnReply = true;
			securityBinding.SecurityHeaderLayout = SecurityHeaderLayout.Lax;
            securityBinding.InitiatorTokenParameters = null; 
            */
             
			this.bindingElements = new BindingElementCollection();
            this.bindingElements.Add(securityBinding);
			this.bindingElements.Add(encoding);
			this.bindingElements.Add(httpsTransport);
		}
		
		public override BindingElementCollection CreateBindingElements() {
			return this.bindingElements.Clone();
		}
		
		public override string Scheme {
			get { return "https"; }
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
