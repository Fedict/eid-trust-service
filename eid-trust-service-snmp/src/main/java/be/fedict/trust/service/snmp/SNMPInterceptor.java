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

package be.fedict.trust.service.snmp;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SNMPInterceptor {

	private static final Log LOG = LogFactory.getLog(SNMPInterceptor.class);

	@AroundInvoke
	public Object invoke(InvocationContext invocationContext) throws Exception {

		process(invocationContext);
		return invocationContext.proceed();
	}

	private void process(InvocationContext invocationContext) {

		Object target = invocationContext.getTarget();
		LOG.debug("process SNMP on " + target.getClass().getCanonicalName());

		SNMP snmp = invocationContext.getMethod().getAnnotation(SNMP.class);
		if (null == snmp) {
			return;
		}

		increment(snmp);
	}

	private void increment(SNMP snmp) {

		LOG.debug("increment counter oid=" + snmp.oid() + " @ service="
				+ snmp.service());
		try {
			for (MBeanServer mBeanServer : MBeanServerFactory
					.findMBeanServer(null)) {
				mBeanServer.invoke(new ObjectName(snmp.service()), "increment",
						new Object[] { snmp.oid() },
						new String[] { "java.lang.String" });

			}
		} catch (Exception e) {
			LOG.error("Failed to contact SNMP Mbean: " + e.getMessage(), e);
		}
	}

}
