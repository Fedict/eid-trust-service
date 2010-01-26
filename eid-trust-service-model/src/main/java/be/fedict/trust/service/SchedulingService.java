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

package be.fedict.trust.service;

import javax.ejb.Local;
import javax.ejb.Timer;

import be.fedict.trust.service.bean.HarvesterMDB;
import be.fedict.trust.service.entity.SchedulingEntity;

/**
 * Scheduler service.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface SchedulingService {

	public static final String JNDI_BINDING = TrustServiceConstants.JNDI_CONTEXT
			+ "/SchedulingServiceBean";

	/**
	 * Timer has timeout, scheduler will notify the {@link HarvesterMDB} and
	 * create a new timer for the next update.
	 * 
	 * @param timer
	 */
	void timeOut(Timer timer);

	/**
	 * Start a new timer for the specified {@link SchedulingEntity}.
	 * 
	 * @param scheduling
	 */
	void startTimer(SchedulingEntity scheduling);

}
