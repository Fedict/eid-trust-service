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

package be.fedict.trust.service.entity;

import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "ts_clock_drift")
public class ClockDriftConfigEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private TimeProtocol timeProtocol;
    private String server;
    private int timeout;
    private int maxClockOffset;

    private boolean enabled = false;

    private String cronSchedule;
    private Date fireDate;

    /**
     * Default constructor.
     */
    public ClockDriftConfigEntity() {

        super();
    }

    /**
     * Main constructor.
     */
    public ClockDriftConfigEntity(String name, TimeProtocol timeProtocol,
                                  String server, int timeout, int maxClockOffset, String cronSchedule) {

        this.name = name;
        this.timeProtocol = timeProtocol;
        this.server = server;
        this.timeout = timeout;
        this.maxClockOffset = maxClockOffset;
        this.cronSchedule = cronSchedule;
    }

    @Id
    public String getName() {

        return this.name;
    }

    public void setName(String name) {

        this.name = name;
    }

    @Enumerated(EnumType.STRING)
    public TimeProtocol getTimeProtocol() {

        return this.timeProtocol;
    }

    public void setTimeProtocol(TimeProtocol timeProtocol) {

        this.timeProtocol = timeProtocol;
    }

    public String getServer() {

        return this.server;
    }

    public void setServer(String server) {

        this.server = server;
    }

    public int getTimeout() {

        return this.timeout;
    }

    public void setTimeout(int timeout) {

        this.timeout = timeout;
    }

    public int getMaxClockOffset() {

        return this.maxClockOffset;
    }

    public void setMaxClockOffset(int maxClockOffset) {

        this.maxClockOffset = maxClockOffset;
    }

    public String getCronSchedule() {

        return this.cronSchedule;
    }

    public void setCronSchedule(String cronSchedule) {

        this.cronSchedule = cronSchedule;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Date getFireDate() {

        return this.fireDate;
    }

    public void setFireDate(Date fireDate) {

        this.fireDate = fireDate;
    }

    @Override
    public String toString() {

        return new ToStringBuilder(this).append("protocol",
                this.timeProtocol.name()).append("server", this.server).append(
                "timeout", this.timeout).append("maxClockOffset",
                this.maxClockOffset).append("cronSchedule", this.cronSchedule).toString();
    }

}
