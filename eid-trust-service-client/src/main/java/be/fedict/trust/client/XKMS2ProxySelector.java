/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2011 FedICT.
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

package be.fedict.trust.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class XKMS2ProxySelector extends ProxySelector {

	private static final Log LOG = LogFactory.getLog(XKMS2ProxySelector.class);

	private final ProxySelector defaultProxySelector;

	private final Map<String, Proxy> proxies;

	public XKMS2ProxySelector(ProxySelector proxySelector) {
		this.defaultProxySelector = proxySelector;
		this.proxies = new HashMap<String, Proxy>();
	}

	@Override
	public List<Proxy> select(URI uri) {
		LOG.debug("select: " + uri);
		String hostname = uri.getHost();
		Proxy proxy = this.proxies.get(hostname);
		if (null != proxy) {
			LOG.debug("using proxy: " + proxy);
			return Collections.singletonList(proxy);
		}
		List<Proxy> proxyList = this.defaultProxySelector.select(uri);
		return proxyList;
	}

	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		this.defaultProxySelector.connectFailed(uri, sa, ioe);
	}

	public void setProxy(String location, String proxyHost, int proxyPort) {
		String hostname;
		try {
			hostname = new URL(location).getHost();
		} catch (MalformedURLException e) {
			throw new RuntimeException("URL error: " + e.getMessage(), e);
		}
		this.proxies.put(hostname, new Proxy(Type.HTTP, new InetSocketAddress(
				proxyHost, proxyPort)));
	}
}
