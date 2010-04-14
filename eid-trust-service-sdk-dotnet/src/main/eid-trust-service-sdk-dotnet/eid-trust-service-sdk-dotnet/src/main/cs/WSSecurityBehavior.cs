using System;
using System.Collections.Generic;
using System.Text;
using System.ServiceModel.Description;
using System.ServiceModel.Channels;
using System.ServiceModel.Dispatcher;
using System.Xml;

namespace eid_trust_service_sdk_dotnet
{
    /// <summary>
    /// Debug WS-Security behaviour, just cuts out the incoming WS-Security header
    /// </summary>
    class WSSecurityBehavior : IEndpointBehavior
    {
        #region IEndpointBehavior Members

        public void AddBindingParameters(ServiceEndpoint endpoint, BindingParameterCollection bindingParameters)
        {
            Console.WriteLine("AddBindingParameters");
            // throw new NotImplementedException();
        }

        public void ApplyClientBehavior(ServiceEndpoint endpoint, ClientRuntime clientRuntime)
        {
            Console.WriteLine("ApplyClientBehaviour");
            clientRuntime.MessageInspectors.Add(new WSSecurityClientMessageInspector());
            // throw new NotImplementedException();
        }

        public void ApplyDispatchBehavior(ServiceEndpoint endpoint, EndpointDispatcher endpointDispatcher)
        {
            Console.WriteLine("ApplyDispatchBehavior");
            // throw new NotImplementedException();
        }

        public void Validate(ServiceEndpoint endpoint)
        {
            Console.WriteLine("Validate");
            // throw new NotImplementedException();
        }

        #endregion
    }

    public class WSSecurityClientMessageInspector : IClientMessageInspector
    {
        #region IClientMessageInspector Members

        public void AfterReceiveReply(ref Message reply, object correlationState)
        {
            Console.WriteLine("AfterReceiveReply");
            int idx = reply.Headers.FindHeader("Security", 
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
            Console.WriteLine("Header index: " + idx);
            XmlDictionaryReader reader = reply.Headers.GetReaderAtHeader(0);
            // Console.WriteLine("WS-Security header: " + reader.g);

            reply.Headers.RemoveAt(0);
        }

        public object BeforeSendRequest(ref Message request, System.ServiceModel.IClientChannel channel)
        {
            Console.WriteLine("BeforeSendRequest");
            return null;
        }

        #endregion
    }
}
