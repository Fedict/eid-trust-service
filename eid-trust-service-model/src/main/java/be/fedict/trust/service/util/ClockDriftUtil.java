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

package be.fedict.trust.service.util;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Date;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;

import be.fedict.trust.NetworkConfig;
import be.fedict.trust.service.entity.ClockDriftConfigEntity;

/**
 * Clock drift detection utility class for the NTP and TSP protocol.
 */
public abstract class ClockDriftUtil {

	private static final Log LOG = LogFactory.getLog(ClockDriftUtil.class);

	public static TimeInfo executeNTP(ClockDriftConfigEntity clockDriftConfig,
			final NetworkConfig networkConfig) throws IOException {

		LOG.debug("clock drift detection: " + clockDriftConfig.toString());

		final InetAddress ntpServerAddress = InetAddress
				.getByName(clockDriftConfig.getServer());

		TimeInfo timeInfo;
		NTPUDPClient client = new NTPUDPClient();
		client.setDefaultTimeout(clockDriftConfig.getTimeout());
		client.open();
		LOG.debug("NTP server: " + ntpServerAddress);
		timeInfo = client.getTime(ntpServerAddress);
		client.close();
		timeInfo.computeDetails();
		return timeInfo;
	}

	public static Date executeTSP(ClockDriftConfigEntity clockDriftConfig,
			NetworkConfig networkConfig) throws IOException, TSPException {

		LOG.debug("clock drift detection: " + clockDriftConfig.toString());

		TimeStampRequestGenerator requestGen = new TimeStampRequestGenerator();

		TimeStampRequest request = requestGen.generate(TSPAlgorithms.SHA1,
				new byte[20], BigInteger.valueOf(100));
		byte[] requestData = request.getEncoded();

		HttpClient httpClient = new HttpClient();

		if (null != networkConfig) {
			httpClient.getHostConfiguration().setProxy(
					networkConfig.getProxyHost(), networkConfig.getProxyPort());
		}

		PostMethod postMethod = new PostMethod(clockDriftConfig.getServer());
		postMethod.setRequestEntity(new ByteArrayRequestEntity(requestData,
				"application/timestamp-query"));

		int statusCode = httpClient.executeMethod(postMethod);
		if (statusCode != HttpStatus.SC_OK) {
			throw new TSPException("Error contacting TSP server "
					+ clockDriftConfig.getServer());
		}

		TimeStampResponse tspResponse = new TimeStampResponse(
				postMethod.getResponseBodyAsStream());
		postMethod.releaseConnection();

		return tspResponse.getTimeStampToken().getTimeStampInfo().getGenTime();
	}
}
