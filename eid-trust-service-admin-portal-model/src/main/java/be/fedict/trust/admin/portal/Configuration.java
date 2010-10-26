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
package be.fedict.trust.admin.portal;

import javax.ejb.Local;
import javax.faces.model.SelectItem;
import java.util.List;

@Local
public interface Configuration {

    /*
      * Lifecycle.
      */
    void destroyCallback();

    void initialize();

    /*
      * Factory
      */
    List<SelectItem> clockDriftProtocolFactory();

    List<SelectItem> keyStoreTypeFactory();

    List<SelectItem> supportedLanguagesFactory();

    /*
      * Accessors
      */
    String getSelectedTab();

    String getProxyHost();

    void setProxyHost(String proxyHost);

    int getProxyPort();

    void setProxyPort(int proxyPort);

    boolean isProxyEnabled();

    void setProxyEnabled(boolean enabled);

    String getClockDriftProtocol();

    void setClockDriftProtocol(String clockDriftProtocol);

    String getClockDriftServer();

    void setClockDriftServer(String clockDriftServer);

    int getClockDriftTimeout();

    void setClockDriftTimeout(int clockDriftTimeout);

    int getClockDriftMaxClockOffset();

    void setClockDriftMaxClockOffset(int clockDriftMaxClockOffset);

    String getClockDriftCronSchedule();

    void setClockDriftCronSchedule(String clockDriftCronSchedule);

    boolean isClockDriftEnabled();

    void setClockDriftEnabled(boolean enabled);

    String getLanguage();

    void setLanguage(String language);

    String getInformationMessage();

    void setInformationMessage(String informationMessage);

    boolean isWsSecuritySigning();

    void setWsSecuritySigning(boolean signing);

    String getWsSecurityKeyStoreType();

    void setWsSecurityKeyStoreType(String type);

    String getWsSecurityKeyStorePath();

    void setWsSecurityKeyStorePath(String path);

    String getWsSecurityKeyStorePassword();

    void setWsSecurityKeyStorePassword(String password);

    String getWsSecurityKeyEntryPassword();

    void setWsSecurityKeyEntryPassword(String password);

    String getWsSecurityAlias();

    void setWsSecurityAlias(String alias);

    /*
      * Actions
      */
    String saveNetworkConfig();

    String saveWSSecurityConfig();

    String saveClockDriftConfig();

    String editInfoMessage();

    String saveInfoMessage();
}
