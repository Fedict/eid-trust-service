/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2010 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package test.integ.be.fedict.trust;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test SOAP Message Context.
 * 
 * @author wvdhaute
 * 
 */
public class TestSOAPMessageContext implements SOAPMessageContext {

	private static final Log LOG = LogFactory
			.getLog(TestSOAPMessageContext.class);

	private SOAPMessage message;

	private final Map<String, Object> properties;

	public TestSOAPMessageContext(SOAPMessage message, boolean outbound) {

		this.message = message;
		this.properties = new HashMap<String, Object>();
		this.properties.put(MessageContext.MESSAGE_OUTBOUND_PROPERTY, outbound);
	}

	public Object[] getHeaders(QName name, JAXBContext context, boolean required) {

		return null;
	}

	public SOAPMessage getMessage() {

		return this.message;
	}

	public Set<String> getRoles() {

		return null;
	}

	public void setMessage(SOAPMessage message) {

		this.message = message;
	}

	public Scope getScope(String scope) {

		return null;
	}

	public void setScope(String scopeName, Scope scope) {

		// empty
	}

	public void clear() {

	}

	public boolean containsKey(Object key) {

		return false;
	}

	public boolean containsValue(Object value) {

		return false;
	}

	public Set<java.util.Map.Entry<String, Object>> entrySet() {

		return null;
	}

	public Object get(Object key) {

		return this.properties.get(key);
	}

	public boolean isEmpty() {

		return false;
	}

	public Set<String> keySet() {

		return null;
	}

	public Object put(String key, Object value) {

		LOG.debug("put: " + key);
		return this.properties.put(key, value);
	}

	public void putAll(Map<? extends String, ? extends Object> t) {

	}

	public Object remove(Object key) {

		return null;
	}

	public int size() {

		return 0;
	}

	public Collection<Object> values() {

		return null;
	}
}