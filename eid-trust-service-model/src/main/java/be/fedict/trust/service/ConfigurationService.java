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

import java.util.List;
import java.util.Locale;

import javax.ejb.Local;

import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.entity.KeyStoreType;
import be.fedict.trust.service.entity.NetworkConfigEntity;
import be.fedict.trust.service.entity.TimeProtocol;
import be.fedict.trust.service.entity.WSSecurityConfigEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.exception.InvalidMaxClockOffsetException;
import be.fedict.trust.service.exception.InvalidTimeoutException;
import be.fedict.trust.service.exception.KeyStoreLoadException;

/**
 * Configuration service.
 * 
 * @author wvdhaute
 */
@Local
public interface ConfigurationService {

	/**
	 * Returns the {@link NetworkConfigEntity}.
	 */
	NetworkConfigEntity getNetworkConfig();

	/**
	 * Save the {@link NetworkConfigEntity}.
	 */
	void saveNetworkConfig(String proxyHost, int proxyPort, boolean enabled);

	/**
	 * Returns the {@link ClockDriftConfigEntity}.
	 */
	ClockDriftConfigEntity getClockDriftDetectionConfig();

	/**
	 * Save the {@link ClockDriftConfigEntity}.
	 */
	void saveClockDriftConfig(TimeProtocol timeProtocol, String server,
			int timeout, int maxClockOffset, String cronSchedule,
			boolean enabled) throws InvalidTimeoutException,
			InvalidMaxClockOffsetException, InvalidCronExpressionException;

	/**
	 * Returns the {@link WSSecurityConfigEntity}.
	 */
	WSSecurityConfigEntity getWSSecurityConfig();

	/**
	 * Save the {@link WSSecurityConfigEntity}.
	 */
	void saveWSSecurityConfig(boolean signing, KeyStoreType keyStoreType,
			String keyStorePath, String keyStorePassword,
			String keyEntryPassword, String alias) throws KeyStoreLoadException;

	/**
	 * List the languages available for the specified key.
	 */
	List<String> listLanguages(String key);

	/**
	 * Return the localization text for specified key and {@link Locale}.
	 * Returns <code>null</code> if not found.
	 */
	String findText(String key, Locale locale);

	/**
	 * Save the specified localization text.
	 */
	void saveText(String key, Locale locale, String text);
}
