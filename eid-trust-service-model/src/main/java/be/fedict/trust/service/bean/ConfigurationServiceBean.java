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

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.ConfigurationService;
import be.fedict.trust.service.KeyStoreUtils;
import be.fedict.trust.service.SchedulingService;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.dao.LocalizationDAO;
import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.entity.KeyStoreType;
import be.fedict.trust.service.entity.LocalizationKeyEntity;
import be.fedict.trust.service.entity.LocalizationTextEntity;
import be.fedict.trust.service.entity.NetworkConfigEntity;
import be.fedict.trust.service.entity.TimeProtocol;
import be.fedict.trust.service.entity.WSSecurityConfigEntity;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.exception.InvalidMaxClockOffsetException;
import be.fedict.trust.service.exception.InvalidTimeoutException;
import be.fedict.trust.service.exception.KeyStoreLoadException;

/**
 * Configuration Service Bean implementation.
 * 
 * @author wvdhaute
 */
@Stateless
public class ConfigurationServiceBean implements ConfigurationService {

	private static final Log LOG = LogFactory
			.getLog(ConfigurationServiceBean.class);

	@EJB
	private ConfigurationDAO configurationDAO;

	@EJB
	private LocalizationDAO localizationDAO;

	@EJB
	private SchedulingService schedulingService;

	@EJB
	private CrlRepositoryServiceBean crlRepositoryServiceBean;
	
	@EJB
	private ServiceIdentityManagerBean serviceIdentityManagerBean;

	/**
	 * {@inheritDoc}
	 */
	public NetworkConfigEntity getNetworkConfig() {

		return this.configurationDAO.getNetworkConfigEntity();
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveNetworkConfig(String proxyHost, int proxyPort,
			boolean enabled) {

		LOG.debug("save network config");
		this.configurationDAO.setNetworkConfig(proxyHost, proxyPort);
		this.configurationDAO.setNetworkConfigEnabled(enabled);

		// reset the CRL cache on new network configuration.
		crlRepositoryServiceBean.resetCachedCrlRepository();
	}

	/**
	 * {@inheritDoc}
	 */
	public ClockDriftConfigEntity getClockDriftDetectionConfig() {

		return this.configurationDAO.getClockDriftConfig();
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveClockDriftConfig(TimeProtocol timeProtocol, String server,
			int timeout, int maxClockOffset, String cronSchedule,
			boolean enabled) throws InvalidTimeoutException,
			InvalidMaxClockOffsetException, InvalidCronExpressionException {

		LOG.debug("save clock drift detection config");

		// input validation
		if (timeout <= 0) {
			throw new InvalidTimeoutException();
		}
		if (maxClockOffset < 0) {
			throw new InvalidMaxClockOffsetException();
		}

		// save
		ClockDriftConfigEntity clockDriftConfig = this.configurationDAO
				.setClockDriftConfig(timeProtocol, server, timeout,
						maxClockOffset, cronSchedule);
		this.configurationDAO.setClockDriftConfigEnabled(enabled);
		if (enabled) {
			this.schedulingService.startTimer(clockDriftConfig);
		} else {
			this.schedulingService.cancelTimers(clockDriftConfig);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> listLanguages(String key) {

		LOG.debug("list languages for: " + key);
		List<String> languages = new LinkedList<String>();
		LocalizationKeyEntity localizationKey = this.localizationDAO
				.findLocalization(key);
		if (null != localizationKey) {
			for (LocalizationTextEntity text : localizationKey.getTexts()) {
				languages.add(text.getLanguage());
			}
		}
		return languages;
	}

	/**
	 * {@inheritDoc}
	 */
	public String findText(String key, Locale locale) {

		LOG.debug("find text for key=" + key + " language="
				+ locale.getLanguage());
		LocalizationKeyEntity localizationKey = this.localizationDAO
				.findLocalization(key);
		for (LocalizationTextEntity text : localizationKey.getTexts()) {
			if (text.getLanguage().equals(locale.getLanguage())) {
				return text.getText();
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveText(String key, Locale locale, String text) {

		LOG.debug("save text for key=" + key + " language="
				+ locale.getLanguage());
		LocalizationKeyEntity localizationKey = this.localizationDAO
				.findLocalization(key);
		for (LocalizationTextEntity localizationText : localizationKey
				.getTexts()) {
			if (localizationText.getLanguage().equals(locale.getLanguage())) {
				localizationText.setText(text);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public WSSecurityConfigEntity getWSSecurityConfig() {

		return this.configurationDAO.getWSSecurityConfig();
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveWSSecurityConfig(boolean signing,
			KeyStoreType keyStoreType, String keyStorePath,
			String keyStorePassword, String keyEntryPassword, String alias)
			throws KeyStoreLoadException {

		/*
		 * Check if valid keystore configuration
		 */
		if (null != keyStorePath) {
			KeyStoreUtils.loadPrivateKeyEntry(keyStoreType, keyStorePath,
					keyStorePassword, keyEntryPassword, alias);
		}

		this.configurationDAO.setWSSecurityConfig(signing, keyStoreType,
				keyStorePath, keyStorePassword, keyEntryPassword, alias);
		
		this.serviceIdentityManagerBean.updateServiceIdentity();
	}
}
