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

package be.fedict.trust.admin.portal.bean;

import be.fedict.trust.admin.portal.AdminConstants;
import be.fedict.trust.admin.portal.Configuration;
import be.fedict.trust.service.ConfigurationService;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.entity.*;
import be.fedict.trust.service.exception.InvalidCronExpressionException;
import be.fedict.trust.service.exception.InvalidMaxClockOffsetException;
import be.fedict.trust.service.exception.InvalidTimeoutException;
import be.fedict.trust.service.exception.KeyStoreLoadException;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.security.Admin;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.log.Log;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.model.SelectItem;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@Stateful
@Name(AdminConstants.ADMIN_SEAM_PREFIX + "config")
@LocalBinding(jndiBinding = AdminConstants.ADMIN_JNDI_CONTEXT
		+ "ConfigurationBean")
public class ConfigurationBean implements Configuration {

	enum ConfigurationTab {
		tab_network, tab_wssec, tab_clock
	}

	@Logger
	private Log log;

	@EJB
	private ConfigurationService configurationService;

	@In
	FacesMessages facesMessages;

	private String proxyHost;
	private int proxyPort;
	private boolean proxyEnabled;

	private String clockDriftProtocol;
	private String clockDriftServer;
	private int clockDriftTimeout;
	private int clockDriftMaxClockOffset;
	private String clockDriftCronSchedule;
	private boolean clockDriftEnabled;

	private boolean wsSecuritySigning;
	private String wsSecurityKeyStoreType;
	private String wsSecurityKeyStorePath;
	private String wsSecurityKeyStorePassword;
	private String wsSecurityKeyEntryPassword;
	private String wsSecurityAlias;

	@In(value = "language", required = false)
	@Out(value = "language", required = false, scope = ScopeType.CONVERSATION)
	private String language;

	@In(value = "selectedTab", required = false)
	@Out(value = "selectedTab", required = false, scope = ScopeType.CONVERSATION)
	private String selectedTab = null;

	private String informationMessage;

	/**
	 * {@inheritDoc}
	 */
	@Remove
	@Destroy
	public void destroyCallback() {

		this.log.debug("#destroy");
	}

	/**
	 * {@inheritDoc}
	 */
	@PostConstruct
	@Begin(join = true)
	public void initialize() {

		this.log.debug("#initialize");
		NetworkConfigEntity networkConfig = this.configurationService
				.getNetworkConfig();
		this.proxyHost = networkConfig.getProxyHost();
		this.proxyPort = networkConfig.getProxyPort();
		this.proxyEnabled = networkConfig.isEnabled();

		ClockDriftConfigEntity clockDriftConfig = this.configurationService
				.getClockDriftDetectionConfig();
		this.clockDriftProtocol = clockDriftConfig.getTimeProtocol().name();
		this.clockDriftServer = clockDriftConfig.getServer();
		this.clockDriftTimeout = clockDriftConfig.getTimeout();
		this.clockDriftMaxClockOffset = clockDriftConfig.getMaxClockOffset();
		this.clockDriftCronSchedule = clockDriftConfig.getCronSchedule();
		this.clockDriftEnabled = clockDriftConfig.isEnabled();

		WSSecurityConfigEntity wsSecurityConfig = this.configurationService
				.getWSSecurityConfig();
		this.wsSecuritySigning = wsSecurityConfig.isSigning();
		this.wsSecurityKeyStoreType = wsSecurityConfig.getKeyStoreType().name();
		this.wsSecurityKeyStorePath = wsSecurityConfig.getKeyStorePath();
		this.wsSecurityKeyStorePassword = wsSecurityConfig
				.getKeyStorePassword();
		this.wsSecurityKeyEntryPassword = wsSecurityConfig
				.getKeyEntryPassword();
		this.wsSecurityAlias = wsSecurityConfig.getAlias();
	}

	/**
	 * {@inheritDoc}
	 */
	@End
	@Admin
	public String saveNetworkConfig() {

		this.log.debug(
				"save network config: proxyHost=#0  proxyPort=#1  enabled=#2",
				this.proxyHost, this.proxyPort, this.proxyEnabled);

		this.configurationService.saveNetworkConfig(proxyHost, proxyPort,
				proxyEnabled);
		this.selectedTab = ConfigurationTab.tab_network.name();
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@End
	@Admin
	public String saveWSSecurityConfig() {

		this.log.debug("save ws security config: signing=#0 type=#1, path=#2 "
				+ "store pw=#3 entry pw=#4 alias=#5", this.wsSecuritySigning,
				this.wsSecurityKeyStoreType, this.wsSecurityKeyStorePath,
				this.wsSecurityKeyStorePassword,
				this.wsSecurityKeyEntryPassword, this.wsSecurityAlias);
		try {
			this.configurationService.saveWSSecurityConfig(
					this.wsSecuritySigning,
					KeyStoreType.valueOf(this.wsSecurityKeyStoreType),
					this.wsSecurityKeyStorePath,
					this.wsSecurityKeyStorePassword,
					this.wsSecurityKeyEntryPassword, this.wsSecurityAlias);
		} catch (KeyStoreLoadException e) {
			this.facesMessages.addToControlFromResourceBundle("wssec_path",
					StatusMessage.Severity.ERROR, "errorLoadKeyStore",
					e.getMessage());
			return null;
		}
		this.selectedTab = ConfigurationTab.tab_wssec.name();
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@End
	@Admin
	public String saveClockDriftConfig() {

		this.log.debug("save clock drift config: protocol=#0 server=#1 "
				+ "timeout=#2 maxClockOffset=#3 "
				+ "clockDriftCronSchedule=#4 enabled=#5",
				this.clockDriftProtocol, this.clockDriftServer,
				this.clockDriftTimeout, this.clockDriftMaxClockOffset,
				this.clockDriftCronSchedule, this.clockDriftEnabled);

		try {
			this.configurationService.saveClockDriftConfig(
					TimeProtocol.valueOf(this.clockDriftProtocol),
					this.clockDriftServer, this.clockDriftTimeout,
					this.clockDriftMaxClockOffset, this.clockDriftCronSchedule,
					this.clockDriftEnabled);
		} catch (InvalidTimeoutException e) {
			this.facesMessages.addToControlFromResourceBundle("timeout",
					StatusMessage.Severity.ERROR, "errorTimeoutInvalid");
			return null;
		} catch (InvalidMaxClockOffsetException e) {
			this.facesMessages.addToControlFromResourceBundle("maxClockOffset",
					StatusMessage.Severity.ERROR, "errorMaxClockOffsetInvalid");
			return null;
		} catch (InvalidCronExpressionException e) {
			this.facesMessages.addToControlFromResourceBundle("cronSchedule",
					StatusMessage.Severity.ERROR, "errorCronExpressionInvalid");
			return null;
		}
		this.selectedTab = ConfigurationTab.tab_clock.name();
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@Begin(join = true)
	public String editInfoMessage() {

		this.log.debug("edit info message for language=#0", this.language);
		this.informationMessage = this.configurationService.findText(
				TrustServiceConstants.INFO_MESSAGE_KEY, new Locale(
						this.language));
		return "edit";
	}

	/**
	 * {@inheritDoc}
	 */
	@End
	@Admin
	public String saveInfoMessage() {

		this.log.debug("save info message for language=#0", this.language);
		this.configurationService.saveText(
				TrustServiceConstants.INFO_MESSAGE_KEY, new Locale(
						this.language), this.informationMessage);
		return "success";
	}

	/**
	 * {@inheritDoc}
	 */
	@Factory("clockDriftProtocols")
	public List<SelectItem> clockDriftProtocolFactory() {

		List<SelectItem> protocols = new LinkedList<SelectItem>();
		for (TimeProtocol protocol : TimeProtocol.values()) {
			protocols.add(new SelectItem(protocol.name(), protocol.name()));
		}
		return protocols;
	}

	/**
	 * {@inheritDoc}
	 */
	@Factory("keyStoreTypes")
	public List<SelectItem> keyStoreTypeFactory() {

		List<SelectItem> keyStoreTypes = new LinkedList<SelectItem>();
		for (KeyStoreType type : KeyStoreType.values()) {
			keyStoreTypes.add(new SelectItem(type.name(), type.name()));
		}
		return keyStoreTypes;
	}

	/**
	 * {@inheritDoc}
	 */
	@Factory("supportedLanguages")
	public List<SelectItem> supportedLanguagesFactory() {

		List<SelectItem> locales = new LinkedList<SelectItem>();
		for (String language : this.configurationService
				.listLanguages(TrustServiceConstants.INFO_MESSAGE_KEY)) {
			locales.add(new SelectItem(language, language));
		}
		return locales;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getSelectedTab() {

		return this.selectedTab;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getProxyHost() {

		return this.proxyHost;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getProxyPort() {

		return this.proxyPort;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isProxyEnabled() {

		return this.proxyEnabled;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setProxyEnabled(boolean proxyEnabled) {

		this.proxyEnabled = proxyEnabled;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setProxyHost(String proxyHost) {

		this.proxyHost = proxyHost;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setProxyPort(int proxyPort) {

		this.proxyPort = proxyPort;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getClockDriftCronSchedule() {

		return this.clockDriftCronSchedule;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getClockDriftMaxClockOffset() {

		return this.clockDriftMaxClockOffset;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getClockDriftProtocol() {

		return this.clockDriftProtocol;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getClockDriftServer() {

		return this.clockDriftServer;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getClockDriftTimeout() {

		return this.clockDriftTimeout;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setClockDriftCronSchedule(String clockDriftCronSchedule) {

		this.clockDriftCronSchedule = clockDriftCronSchedule;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setClockDriftMaxClockOffset(int clockDriftMaxClockOffset) {

		this.clockDriftMaxClockOffset = clockDriftMaxClockOffset;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setClockDriftProtocol(String clockDriftProtocol) {

		this.clockDriftProtocol = clockDriftProtocol;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setClockDriftServer(String clockDriftServer) {

		this.clockDriftServer = clockDriftServer;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setClockDriftTimeout(int clockDriftTimeout) {

		this.clockDriftTimeout = clockDriftTimeout;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isClockDriftEnabled() {

		return this.clockDriftEnabled;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setClockDriftEnabled(boolean clockDriftEnabled) {

		this.clockDriftEnabled = clockDriftEnabled;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getLanguage() {

		return this.language;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLanguage(String language) {

		this.language = language;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getInformationMessage() {

		return this.informationMessage;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setInformationMessage(String informationMessage) {

		this.informationMessage = informationMessage;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getWsSecurityKeyStorePassword() {

		return this.wsSecurityKeyStorePassword;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getWsSecurityKeyStorePath() {

		return this.wsSecurityKeyStorePath;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getWsSecurityKeyStoreType() {

		return this.wsSecurityKeyStoreType;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isWsSecuritySigning() {

		return this.wsSecuritySigning;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setWsSecurityKeyStorePassword(String password) {

		this.wsSecurityKeyStorePassword = password;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setWsSecurityKeyStorePath(String path) {

		this.wsSecurityKeyStorePath = path;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setWsSecuritySigning(boolean signing) {

		this.wsSecuritySigning = signing;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setWsSecurityKeyStoreType(String type) {

		this.wsSecurityKeyStoreType = type;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getWsSecurityAlias() {

		return this.wsSecurityAlias;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getWsSecurityKeyEntryPassword() {

		return this.wsSecurityKeyEntryPassword;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setWsSecurityAlias(String alias) {

		this.wsSecurityAlias = alias;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setWsSecurityKeyEntryPassword(String password) {

		this.wsSecurityKeyEntryPassword = password;
	}
}
