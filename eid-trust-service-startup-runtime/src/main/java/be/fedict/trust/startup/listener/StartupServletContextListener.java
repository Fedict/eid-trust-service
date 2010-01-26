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

package be.fedict.trust.startup.listener;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.InitializationService;

public class StartupServletContextListener implements ServletContextListener {

	private static final Log LOG = LogFactory
			.getLog(StartupServletContextListener.class);

	public void contextInitialized(ServletContextEvent event) {

		LOG.debug("context initialized");

		try {
			InitializationService initializationService = (InitializationService) new InitialContext()
					.lookup(InitializationService.JNDI_BINDING);
			initializationService.initialize();
		} catch (NamingException e) {
			LOG.error("Naming exception thrown: " + e.getMessage(), e);
		}
	}

	public void contextDestroyed(ServletContextEvent event) {

		LOG.debug("context destroyed");
	}
}
