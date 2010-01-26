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

package be.fedict.trust.jaas;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.acl.Group;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;

import be.fedict.trust.service.AdminAuthorizationService;
import be.fedict.trust.service.TrustServiceConstants;

/**
 * JAAS login module that performs authentication and authorization used by the
 * trust service administrator security domain.
 * 
 * @author wvdhaute
 * 
 */
public class AdminLoginModule implements LoginModule {

	private static final Log LOG = LogFactory.getLog(AdminLoginModule.class);

	private Subject subject;

	private CallbackHandler callbackHandler;

	private Principal authenticatedPrincipal;

	/**
	 * {@inheritDoc}
	 */
	public boolean abort() {

		LOG.debug("abort");
		authenticatedPrincipal = null;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean commit() throws LoginException {

		LOG.debug("commit");

		Set<Principal> principals = subject.getPrincipals();
		if (null == authenticatedPrincipal)
			throw new LoginException(
					"authenticated principal should be not null");
		// authenticate
		principals.add(authenticatedPrincipal);

		// make JBoss happy
		Group callerPrincipalGroup = getGroup("CallerPrincipal", principals);
		callerPrincipalGroup.addMember(authenticatedPrincipal);

		// authorize
		Group rolesGroup = getGroup("Roles", principals);

		rolesGroup.addMember(new SimplePrincipal(
				TrustServiceConstants.ADMIN_ROLE));

		LOG.debug("committed");

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void initialize(Subject newSubject,
			CallbackHandler newCallbackHandler,
			@SuppressWarnings("unchecked") Map sharedState,
			@SuppressWarnings("unchecked") Map options) {

		LOG.debug("initialize");

		subject = newSubject;
		callbackHandler = newCallbackHandler;
		LOG.debug("subject class: " + subject.getClass().getName());
		LOG.debug("callback handler class: "
				+ callbackHandler.getClass().getName());
	}

	private Group getGroup(String groupName, Set<Principal> principals) {

		Iterator<?> iter = principals.iterator();
		while (iter.hasNext()) {
			Object next = iter.next();
			if (next instanceof Group == false) {
				continue;
			}
			Group group = (Group) next;
			if (group.getName().equals(groupName))
				return group;
		}
		// If we did not find a group create one
		Group group = new SimpleGroup(groupName);
		principals.add(group);
		return group;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean login() throws LoginException {

		LOG.debug("login: " + this);
		// retrieve the certificate credential
		PasswordCallback passwordCallback = new PasswordCallback(
				"X509 application certificate in Hex", false);
		NameCallback nameCallback = new NameCallback("admin id");
		Callback[] callbacks = new Callback[] { passwordCallback, nameCallback };

		try {
			callbackHandler.handle(callbacks);
		} catch (IOException e) {
			String msg = "IO error: " + e.getMessage();
			LOG.error(msg);
			throw new LoginException(msg);
		} catch (UnsupportedCallbackException e) {
			String msg = "unsupported callback: " + e.getMessage();
			LOG.error(msg);
			throw new LoginException(msg);
		}

		char[] password = passwordCallback.getPassword();
		List<X509Certificate> authnCertChain;
		try {
			authnCertChain = toX509CertificateChain(password);
		} catch (Exception e) {
			throw new LoginException("X509 decoding error: " + e.getMessage());
		}

		// authenticate
		String userId;
		AdminAuthorizationService adminAuthorizationService = getAdminAuthorizationService();
		try {
			userId = adminAuthorizationService.authenticate(authnCertChain);
		} catch (NoSuchAlgorithmException e) {
			throw new FailedLoginException("Failed to login");
		} catch (InvalidKeySpecException e) {
			throw new FailedLoginException("Failed to login");
		} catch (CertPathValidatorException e) {
			throw new FailedLoginException(
					"Failed to login: certificate invalid");
		}

		if (null == userId)
			throw new FailedLoginException("Administrator not found");

		String expectedUserId = nameCallback.getName();
		if (userId != expectedUserId)
			throw new FailedLoginException("user ID not correct");

		authenticatedPrincipal = new SimplePrincipal(nameCallback.getName());
		LOG.debug("login: " + nameCallback.getName());
		return true;
	}

	private AdminAuthorizationService getAdminAuthorizationService()
			throws LoginException {

		try {
			AdminAuthorizationService adminAuthorizationService = (AdminAuthorizationService) new InitialContext()
					.lookup(AdminAuthorizationService.JNDI_BINDING);
			return adminAuthorizationService;
		} catch (NamingException e) {
			throw new LoginException("JNDI lookup error: " + e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean logout() throws LoginException {

		LOG.debug("logout");
		Set<Principal> principals = subject.getPrincipals();
		if (null == authenticatedPrincipal)
			throw new LoginException(
					"authenticated principal should not be null");
		boolean result = principals.remove(authenticatedPrincipal);
		if (!result)
			throw new LoginException("could not remove authenticated principal");
		/*
		 * Despite the fact that JBoss AbstractServerLoginModule is not removing
		 * the roles on the subject, we clear here all data on the subject.
		 */
		subject.getPrincipals().clear();
		subject.getPublicCredentials().clear();
		subject.getPrivateCredentials().clear();
		return true;
	}

	@SuppressWarnings("unchecked")
	private static List<X509Certificate> toX509CertificateChain(char[] password)
			throws DecoderException, CertificateException {

		byte[] encodedCertificate = Hex.decodeHex(password);
		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		ByteArrayInputStream inputStream = new ByteArrayInputStream(
				encodedCertificate);
		return (List<X509Certificate>) certificateFactory
				.generateCertificates(inputStream);
	}

}
