/*
 * eID Trust Service Project.
 * Copyright (C) 2009 FedICT.
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

import java.io.Serializable;
import java.util.Date;

import javax.ejb.TimerHandle;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
@Table(name = "clock_drift")
public class ClockDriftConfigEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;

	private TimeProtocol timeProtocol;
	private String server;
	private int timeout;
	private int maxClockOffset;

	private String cron;
	private TimerHandle timerHandle;
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
			String server, int timeout, int maxClockOffset, String cron) {

		this.name = name;
		this.timeProtocol = timeProtocol;
		this.server = server;
		this.timeout = timeout;
		this.maxClockOffset = maxClockOffset;
		this.cron = cron;
	}

	@Id
	public String getName() {

		return this.name;
	}

	public void setName(String name) {

		this.name = name;
	}

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

	public String getCron() {

		return this.cron;
	}

	public void setCron(String cron) {

		this.cron = cron;
	}

	@Lob
	public TimerHandle getTimerHandle() {

		return this.timerHandle;
	}

	public void setTimerHandle(TimerHandle timerHandle) {

		this.timerHandle = timerHandle;
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
				this.maxClockOffset).toString();
	}

}
