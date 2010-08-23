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

import be.fedict.trust.NetworkConfig;
import be.fedict.trust.service.ClockDriftService;
import be.fedict.trust.service.dao.AuditDAO;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.util.ClockDriftUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ntp.TimeInfo;
import org.bouncycastle.tsp.TSPException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.util.Date;

/**
 * Clock drift detection service bean implementation.
 *
 * @author wvdhaute
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

        ClockDriftConfigEntity clockDriftConfig = this.configurationDAO
                .getClockDriftConfig();
        NetworkConfig networkConfig = this.configurationDAO.getNetworkConfig();

        LOG.debug("clock drift detection: " + clockDriftConfig.toString());

        long offset = 0;
        switch (clockDriftConfig.getTimeProtocol()) {
            case NTP: {

                try {
                    TimeInfo timeInfo = ClockDriftUtil.executeNTP(clockDriftConfig, networkConfig);
                    offset = timeInfo.getOffset();
                } catch (IOException e) {
                    this.auditDAO.logAudit("Error contacting NTP server "
                            + clockDriftConfig.getServer() + " (msg=" + e.getMessage() + ")");
                }
                break;
            }
            case TSP: {

                try {
                    Date now = new Date();
                    offset = ClockDriftUtil.executeTSP(clockDriftConfig, networkConfig).getTime() - now.getTime();
                } catch (IOException e) {
                    this.auditDAO.logAudit("Error contacting NTP server "
                            + clockDriftConfig.getServer() + " (msg=" + e.getMessage() + ")");
                } catch (TSPException e) {
                    this.auditDAO.logAudit("Error contacting NTP server "
                            + clockDriftConfig.getServer() + " (msg=" + e.getMessage() + ")");
                }
                break;
            }
        }

        LOG.debug("clock offset (ms): " + offset);
        if (Math.abs(offset) > clockDriftConfig.getMaxClockOffset()) {
            this.auditDAO.logAudit("Maximum clock offset reached: "
                    + Math.abs(offset) + " > "
                    + clockDriftConfig.getMaxClockOffset());
        }
    }
}
