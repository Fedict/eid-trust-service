using System;
using System.Collections.Generic;
using System.IdentityModel;
using System.IdentityModel.Claims;
using System.IdentityModel.Policy;
using System.ServiceModel;
using System.ServiceModel.Security;

namespace eid_trust_service_sdk_dotnet
{
	/// <summary>
	/// This IdentityVerifier is used by the LinkIDBinding due to the fact that the AsymmetricSecurityBinding
	/// does an extra check on the endpoint DNS claim.
	/// </summary>
	public class DnsIdentityVerifier : IdentityVerifier
	{
		DnsEndpointIdentity expectedIdentity;
		
		public DnsIdentityVerifier(DnsEndpointIdentity expectedIdentity) {
			this.expectedIdentity = expectedIdentity;
		}
		
		public override bool CheckAccess(EndpointIdentity identity, AuthorizationContext authzContext) {
			List<Claim> dnsClaims = new List<Claim>();
			foreach( ClaimSet claimSet in authzContext.ClaimSets ) {
				foreach( Claim claim in claimSet ) {
					if ( ClaimTypes.Dns == claim.ClaimType ) {
						dnsClaims.Add(claim);
					}
				}
			}
			if ( 1 != dnsClaims.Count ) {
				throw new InvalidOperationException(String.Format("Found {0} DNS claims in authorization context.", dnsClaims.Count));
			}
			return String.Equals((string) this.expectedIdentity.IdentityClaim.Resource, (string) dnsClaims[0].Resource, StringComparison.OrdinalIgnoreCase);
		}
		
		public override bool TryGetIdentity(EndpointAddress reference, out EndpointIdentity identity) {
			identity = this.expectedIdentity;
			return true;
		}
	}
}
