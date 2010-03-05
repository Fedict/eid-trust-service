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

import javax.ejb.Local;

import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.entity.NetworkConfigEntity;
import be.fedict.trust.service.entity.TimeProtocol;
import be.fedict.trust.service.exception.InvalidCronExpressionException;

/**
 * Configuration service.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface ConfigurationService {

	/**
	 * Returns the {@link NetworkConfigEntity}.
	 */
	NetworkConfigEntity getNetworkConfig();

	/**
	 * Save the {@link NetworkConfigEntity}.
	 * 
	 * @param proxyHost
	 * @param proxyPort
	 * @param enabled
	 */
	void saveNetworkConfig(String proxyHost, int proxyPort, boolean enabled);

	/**
	 * Returns the {@link ClockDriftConfigEntity}.
	 */
	ClockDriftConfigEntity getClockDriftDetectionConfig();

	/**
	 * Save the {@link ClockDriftConfigEntity}.
	 * 
	 * @param timeProtocol
	 * @param server
	 * @param timeout
	 * @param maxClockOffset
	 * @param cron
	 * 
	 * @throws InvalidCronExpressionException
	 */
	void saveClockDriftConfig(TimeProtocol timeProtocol, String server,
			int timeout, int maxClockOffset, String cron)
			throws InvalidCronExpressionException;
}
