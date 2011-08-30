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

package be.fedict.trust.service.dao;

import javax.ejb.Local;

import be.fedict.trust.NetworkConfig;
import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.entity.KeyStoreType;
import be.fedict.trust.service.entity.NetworkConfigEntity;
import be.fedict.trust.service.entity.TimeProtocol;
import be.fedict.trust.service.entity.WSSecurityConfigEntity;

/**
 * Configuration DAO.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface ConfigurationDAO {

	/**
	 * Returns the {@link NetworkConfigEntity} or <code>null</code> if not
	 * found.
	 */
	NetworkConfigEntity findNetworkConfigEntity();

	/**
	 * Returns the {@link NetworkConfigEntity}. Creates if not existing.
	 */
	NetworkConfigEntity getNetworkConfigEntity();

	/**
	 * Returns the {@link NetworkConfig} if enabled. If not returns
	 * <code>null</code>.
	 */
	NetworkConfig getNetworkConfig();

	/**
	 * Add/update the {@link NetworkConfigEntity}.
	 * 
	 * @param proxyHost
	 * @param proxyPort
	 */
	NetworkConfigEntity setNetworkConfig(String proxyHost, int proxyPort);

	/**
	 * Enable/disable the {@link NetworkConfigEntity}.
	 * 
	 * @param enabled
	 */
	void setNetworkConfigEnabled(boolean enabled);

	/**
	 * Returns the {@link ClockDriftConfigEntity}. Returns <code>null</code> if
	 * not found.
	 */
	ClockDriftConfigEntity findClockDriftConfig();

	/**
	 * Returns the {@link ClockDriftConfigEntity}. Creates if not existing.
	 */
	ClockDriftConfigEntity getClockDriftConfig();

	/**
	 * Add/update the {@link ClockDriftConfigEntity}.
	 * 
	 * @param timeProtocol
	 *            the {@link TimeProtocol} to be used.
	 * @param server
	 *            NTP server path or URL to send the TSP request to
	 * @param timeout
	 *            used by the NTP client as timeout to the NTP server, in ms
	 * @param maxClockOffset
	 *            maximum clock offset accepted, in ms
	 * @param cronSchedule
	 *            cron schedule.
	 * @return
	 */
	ClockDriftConfigEntity setClockDriftConfig(TimeProtocol timeProtocol,
			String server, int timeout, int maxClockOffset, String cronSchedule);

	/**
	 * Enable/disable the {@link ClockDriftConfigEntity}.
	 * 
	 * @param enabled
	 */
	void setClockDriftConfigEnabled(boolean enabled);

	/**
	 * Returns the {@link WSSecurityConfigEntity). Will create if not existing.
	 */
	WSSecurityConfigEntity getWSSecurityConfig();

	/**
	 * Returns the {@link WSSecurityConfigEntity) or <code>null</code> if not
	 * found.
	 */
	WSSecurityConfigEntity findWSSecurityConfig();

	/**
	 * Add/update the {@link WSSecurityConfigEntity}.
	 * 
	 * @param signing
	 * @param keyStoreType
	 * @param keyStorePath
	 * @param keyStorePassword
	 * @param keyEntryPassword
	 * @param alias
	 */
	WSSecurityConfigEntity setWSSecurityConfig(boolean signing,
			KeyStoreType keyStoreType, String keyStorePath,
			String keyStorePassword, String keyEntryPassword, String alias);
}
