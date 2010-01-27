/*
 * eID Trust Service Project.
 * Copyright (C) 2009 FedICT.
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

package be.fedict.trust.admin.filter;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.security.auth.callback.UsernamePasswordHandler;

import be.fedict.eid.applet.service.impl.handler.IdentityDataMessageHandler;
import be.fedict.trust.service.AdminAuthorizationService;

/**
 * JAAS login servlet filter.
 * 
 * Takes the Belgian eID authentication {@link X509Certificate} from the HTTP
 * session and uses it to perform a JAAS login. It also takes care of a proper
 * JAAS logout.
 */
public class JAASLoginFilter implements Filter {

	private static final Log LOG = LogFactory.getLog(JAASLoginFilter.class);

	private static final String USERID_ATTRIBUTE = "userId";
	private static final String AUTHN_CERT_ATTRIBUTE = JAASLoginFilter.class
			.getName()
			+ ".AUTHN_CERT";

	public static final String JAAS_LOGIN_CONTEXT_ATTRIBUTE = JAASLoginFilter.class
			.getName()
			+ ".LOGIN_CONTEXT";

	public static final String LOGIN_CONTEXT_PARAM = "LoginContextName";

	public static final String LOGIN_PATH_PARAM = "LoginPath";
	public static final String MAIN_PATH_PARAM = "MainPath";

	/**
	 * The default JAAS login context is 'client-login'. This is what JBoss AS
	 * expects of EJB clients to use for login.
	 */
	private static final String DEFAULT_LOGIN_CONTEXT = "client-login";

	private String loginContextName;

	private String loginPath;
	private String mainPath;

	public void init(FilterConfig config) {

		loginContextName = getInitParameter(config, LOGIN_CONTEXT_PARAM,
				DEFAULT_LOGIN_CONTEXT);
		LOG.debug("JAAS login context: " + loginContextName);
		loginPath = getInitParameter(config, LOGIN_PATH_PARAM, null);
		LOG.debug("LoginPath: " + loginPath);
		mainPath = getInitParameter(config, MAIN_PATH_PARAM, null);
		LOG.debug("MainPath: " + mainPath);
	}

	private String getInitParameter(FilterConfig config, String param,
			String defaultValue) {

		String value = config.getInitParameter(param);
		if (null == value) {
			value = defaultValue;
		}
		return value;
	}

	public void destroy() {

		LOG.debug("destroy");
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpServletResponse httpServletResponse = (HttpServletResponse) response;
		if (!login(httpServletRequest, httpServletResponse))
			return;
		try {
			chain.doFilter(request, response);
		} finally {
			logout(request);
		}
	}

	private boolean login(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		LOG.debug("login");
		String userId = (String) request.getSession().getAttribute(
				USERID_ATTRIBUTE);
		X509Certificate authnCertificate = (X509Certificate) request
				.getSession()
				.getAttribute(
						IdentityDataMessageHandler.AUTHN_CERT_SESSION_ATTRIBUTE);

		if (null == userId && null == authnCertificate) {
			LOG.debug("not authenticated yet");
			String requestPath = request.getContextPath()
					+ request.getServletPath();
			if (null != mainPath && !loginPath.equals(requestPath)
					&& !mainPath.equals(requestPath)) {
				LOG.debug("redirect to " + mainPath + ", requestPath: "
						+ requestPath);
				response.sendRedirect(mainPath);
				return false;
			}
			return true;
		}

		login(request, loginContextName);
		return true;
	}

	private static void login(HttpServletRequest request,
			String loginContextName) {

		try {
			String userId = (String) request.getSession().getAttribute(
					USERID_ATTRIBUTE);
			X509Certificate authnCert = (X509Certificate) request.getSession()
					.getAttribute(AUTHN_CERT_ATTRIBUTE);

			if (null == userId && null == authnCert) {
				authnCert = (X509Certificate) request
						.getSession()
						.getAttribute(
								IdentityDataMessageHandler.AUTHN_CERT_SESSION_ATTRIBUTE);

				AdminAuthorizationService adminAuthorizationService = getAdminAuthorizationService();
				userId = adminAuthorizationService.authenticate(authnCert);

				request.getSession().setAttribute(USERID_ATTRIBUTE, userId);
				request.getSession().setAttribute(AUTHN_CERT_ATTRIBUTE,
						authnCert);
			}

			UsernamePasswordHandler handler = new UsernamePasswordHandler(
					userId, toPassword(authnCert));

			LoginContext loginContext = new LoginContext(loginContextName,
					handler);
			LOG.debug("login to " + loginContextName + " with " + userId
					+ " for " + request.getRequestURL());
			loginContext.login();
			request.setAttribute(JAAS_LOGIN_CONTEXT_ATTRIBUTE, loginContext);
		} catch (LoginException e) {
			LOG.error("login error: " + e.getMessage(), e);
		} catch (CertificateEncodingException e) {
			LOG.error("login error: " + e.getMessage(), e);
		}
	}

	private static AdminAuthorizationService getAdminAuthorizationService()
			throws LoginException {

		try {
			AdminAuthorizationService adminAuthorizationService = (AdminAuthorizationService) new InitialContext()
					.lookup(AdminAuthorizationService.JNDI_BINDING);
			return adminAuthorizationService;
		} catch (NamingException e) {
			throw new LoginException("JNDI lookup error: " + e.getMessage());
		}
	}

	private static char[] toPassword(X509Certificate certificate)
			throws CertificateEncodingException {

		return Hex.encodeHex(certificate.getEncoded());
	}

	private static void logout(ServletRequest request) {

		LoginContext loginContext = (LoginContext) request
				.getAttribute(JAAS_LOGIN_CONTEXT_ATTRIBUTE);
		if (loginContext == null)
			return;
		try {
			LOG.debug("logout");
			loginContext.logout();
		} catch (LoginException e) {
			LOG.error("logout error: " + e.getMessage(), e);
		}
	}
}
