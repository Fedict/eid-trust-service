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

package test.integ.be.fedict.clockdrift;

import be.fedict.trust.NetworkConfig;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.entity.TimeProtocol;
import be.fedict.trust.service.util.ClockDriftUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ntp.TimeInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import java.security.Security;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * eID Trust Service Clock Drift detection integration Tests.
 * 
 * @author wvdhaute
 */
public class ClockDriftDetectionTest {

	private static final Log LOG = LogFactory
			.getLog(ClockDriftDetectionTest.class);

	// private static final NetworkConfig NETWORK_CONFIG = new NetworkConfig(
	// "proxy.yourict.net", 8080);
	// private static final NetworkConfig NETWORK_CONFIG = null;
	private static final NetworkConfig NETWORK_CONFIG = new NetworkConfig(
			"127.0.0.1", 8080);

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testClockDriftNTP() throws Exception {

		// Setup data
		ClockDriftConfigEntity clockDriftConfig = new ClockDriftConfigEntity(
				"test", TimeProtocol.NTP,
				TrustServiceConstants.CLOCK_DRIFT_NTP_SERVER,
				TrustServiceConstants.CLOCK_DRIFT_TIMEOUT,
				TrustServiceConstants.CLOCK_DRIFT_MAX_CLOCK_OFFSET, null);

		// Operate
		TimeInfo timeInfo = ClockDriftUtil.executeNTP(clockDriftConfig,
				NETWORK_CONFIG);

		// Verify
		assertNotNull(timeInfo);
		LOG.debug("offset=" + timeInfo.getOffset());

		assertFalse(Math.abs(timeInfo.getOffset()) > clockDriftConfig
				.getMaxClockOffset());
	}

	@Test
	public void testClockDriftTSP() throws Exception {

		// Setup data
		ClockDriftConfigEntity clockDriftConfig = new ClockDriftConfigEntity(
				"test", TimeProtocol.TSP,
				"http://www.cryptopro.ru/tsp/tsp.srf",
				TrustServiceConstants.CLOCK_DRIFT_TIMEOUT, 1000 * 60 * 5, null);
		Date now = new Date();

		// Operate
		Date date = ClockDriftUtil.executeTSP(clockDriftConfig, NETWORK_CONFIG);

		// Verify
		assertNotNull(date);
		LOG.debug("now: " + now.toString() + " date=" + date.toString());
		long offset = date.getTime() - now.getTime();
		assertFalse(Math.abs(offset) > clockDriftConfig.getMaxClockOffset());
	}
}
