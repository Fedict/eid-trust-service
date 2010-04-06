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

package be.fedict.trust.service.bean;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.DatagramSocketFactory;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;

import be.fedict.trust.NetworkConfig;
import be.fedict.trust.service.ClockDriftService;
import be.fedict.trust.service.dao.AuditDAO;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.entity.ClockDriftConfigEntity;

/**
 * Clock drift detection service bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
public class ClockDriftDetectorServiceBean implements ClockDriftService {

	private static final Log LOG = LogFactory
			.getLog(ClockDriftDetectorServiceBean.class);

	@EJB
	private ConfigurationDAO configurationDAO;

	@EJB
	private AuditDAO auditDAO;

	/**
	 * {@inheritDoc}
	 */
	public void execute() {

		LOG.debug("execute");

		ClockDriftConfigEntity clockDriftConfig = this.configurationDAO
				.getClockDriftConfig();
		NetworkConfig networkConfig = this.configurationDAO.getNetworkConfig();

		switch (clockDriftConfig.getTimeProtocol()) {
		case NTP: {
			executeNTP(clockDriftConfig, networkConfig);
			break;
		}
		case TSP: {
			executeTSP(clockDriftConfig, networkConfig);
			break;
		}
		}
	}

	private void executeNTP(ClockDriftConfigEntity clockDriftConfig,
			final NetworkConfig networkConfig) {

		LOG.debug("clock drift detection: " + clockDriftConfig.toString());

		TimeInfo timeInfo;
		try {
			NTPUDPClient client = new NTPUDPClient();
			if (null != networkConfig) {

				SocketAddress addr = new InetSocketAddress(networkConfig
						.getProxyHost(), networkConfig.getProxyPort());
				final Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);

				client.setDatagramSocketFactory(new DatagramSocketFactory() {

					public DatagramSocket createDatagramSocket(int port,
							InetAddress laddr) throws SocketException {

						return new DatagramSocket(proxy.address());
					}

					public DatagramSocket createDatagramSocket(int port)
							throws SocketException {

						return new DatagramSocket(proxy.address());
					}

					public DatagramSocket createDatagramSocket()
							throws SocketException {

						return new DatagramSocket(proxy.address());
					}
				});
			}

			client.setDefaultTimeout(clockDriftConfig.getTimeout());
			client.open();
			InetAddress ntpServerAddress = InetAddress
					.getByName(clockDriftConfig.getServer());
			LOG.debug("NTP server: " + ntpServerAddress);
			timeInfo = client.getTime(ntpServerAddress);
			client.close();
			timeInfo.computeDetails();
			Long offset = timeInfo.getOffset();
			LOG.debug("clock offset (ms): " + offset);
			if (Math.abs(offset) > clockDriftConfig.getMaxClockOffset()) {
				this.auditDAO.logAudit("Maximum clock offset reached: "
						+ Math.abs(offset) + " > "
						+ clockDriftConfig.getMaxClockOffset());
			}
		} catch (SocketException e) {
			this.auditDAO.logAudit("Error contacting NTP server "
					+ clockDriftConfig.getServer());
		} catch (UnknownHostException e) {
			this.auditDAO.logAudit("Error contacting NTP server "
					+ clockDriftConfig.getServer());
		} catch (IOException e) {
			this.auditDAO.logAudit("Error contacting NTP server "
					+ clockDriftConfig.getServer());
		}
	}

	private void executeTSP(ClockDriftConfigEntity clockDriftConfig,
			NetworkConfig networkConfig) {

		LOG.debug("clock drift detection: " + clockDriftConfig.toString());

		try {
			TimeStampRequestGenerator requestGen = new TimeStampRequestGenerator();

			TimeStampRequest request = requestGen.generate(TSPAlgorithms.SHA1,
					new byte[20], BigInteger.valueOf(100));
			byte[] requestData = request.getEncoded();

			HttpClient httpClient = new HttpClient();

			if (null != networkConfig) {
				httpClient.getHostConfiguration().setProxy(
						networkConfig.getProxyHost(),
						networkConfig.getProxyPort());
			}

			PostMethod postMethod = new PostMethod(clockDriftConfig.getServer());
			postMethod.setRequestEntity(new ByteArrayRequestEntity(requestData,
					"application/timestamp-query"));

			int statusCode = httpClient.executeMethod(postMethod);
			if (statusCode != HttpStatus.SC_OK) {
				this.auditDAO.logAudit("Error contacting TSP server "
						+ clockDriftConfig.getServer());
				return;
			}

			TimeStampResponse tspResponse = new TimeStampResponse(postMethod
					.getResponseBodyAsStream());
			postMethod.releaseConnection();

			Date now = new Date();
			long offset = tspResponse.getTimeStampToken().getTimeStampInfo()
					.getGenTime().getTime()
					- now.getTime();
			if (Math.abs(offset) > clockDriftConfig.getMaxClockOffset()) {
				this.auditDAO.logAudit("Maximum clock offset reached: "
						+ Math.abs(offset) + " > "
						+ clockDriftConfig.getMaxClockOffset());
			}
		} catch (IOException e) {
			this.auditDAO.logAudit("Error contacting TSP server "
					+ clockDriftConfig.getServer());
		} catch (TSPException e) {
			this.auditDAO.logAudit("Error contacting TSP server "
					+ clockDriftConfig.getServer());
		}
	}
}
