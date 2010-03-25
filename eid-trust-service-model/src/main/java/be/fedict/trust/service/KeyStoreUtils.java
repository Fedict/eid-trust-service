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

package be.fedict.trust.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Security;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import be.fedict.trust.service.entity.KeyStoreType;
import be.fedict.trust.service.entity.WSSecurityConfigEntity;
import be.fedict.trust.service.exception.KeyStoreLoadException;

/**
 * Key Store utility class
 * 
 * @author wvdhaute
 */
public final class KeyStoreUtils {

	private static final Log LOG = LogFactory.getLog(KeyStoreUtils.class);

	static {
		if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	public static PrivateKeyEntry loadPrivateKeyEntry(
			WSSecurityConfigEntity wsSecurityConfig)
			throws KeyStoreLoadException {

		return loadPrivateKeyEntry(wsSecurityConfig.getKeyStoreType(),
				wsSecurityConfig.getKeyStorePath(), wsSecurityConfig
						.getKeyStorePassword(), wsSecurityConfig
						.getKeyEntryPassword(), wsSecurityConfig.getAlias());
	}

	public static PrivateKeyEntry loadPrivateKeyEntry(KeyStoreType type,
			String path, String storePassword, String entryPassword,
			String alias) throws KeyStoreLoadException {

		LOG.debug("load keystore");
		InputStream keyStoreStream;
		try {
			keyStoreStream = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			throw new KeyStoreLoadException(
					"Can't load keystore from config-specified location: "
							+ path, e);
		}

		/* Find the keystore. */
		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance(type.name());
		} catch (Exception e) {
			throw new KeyStoreLoadException("keystore instance not available: "
					+ e.getMessage(), e);
		}

		/* Open the keystore and find the key entry. */
		try {
			keyStore.load(keyStoreStream, storePassword.toCharArray());
		} catch (Exception e) {
			throw new KeyStoreLoadException("keystore load error: "
					+ e.getMessage(), e);
		}
		Enumeration<String> aliases;
		try {
			aliases = keyStore.aliases();
		} catch (KeyStoreException e) {
			throw new KeyStoreLoadException("could not get aliases: "
					+ e.getMessage(), e);
		}
		if (!aliases.hasMoreElements())
			throw new KeyStoreLoadException("keystore is empty");

		try {
			if (!keyStore.isKeyEntry(alias))
				throw new KeyStoreLoadException("not key entry: " + alias);
		} catch (KeyStoreException e) {
			throw new KeyStoreLoadException("key store error: "
					+ e.getMessage(), e);
		}

		/* Get the private key entry. */
		try {
			PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) keyStore
					.getEntry(alias, new KeyStore.PasswordProtection(
							entryPassword.toCharArray()));
			return privateKeyEntry;
		} catch (Exception e) {
			throw new KeyStoreLoadException("error retrieving key: "
					+ e.getMessage(), e);
		}
	}

}
