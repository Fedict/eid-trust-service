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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * SNMP interceptor.
 * 
 * In case the {@link SNMP} annotation is put on the method, each invocation
 * will trigger an increment on the value associated with the configured SNMP
 * oid.
 * 
 * In case the {@link SNMP} annotation is put on a field, before an invocation
 * in one of the classes methods, the current value associated with the SNMP oid
 * will be fetched and injected in the field. After completion of the method,
 * the updated value will be checked and if changed update the SNMP value.
 * 
 * @author wvdhaute
 * 
 */
public class SNMPInterceptor {

	private static final Log LOG = LogFactory.getLog(SNMPInterceptor.class);

	@AroundInvoke
	public Object invoke(InvocationContext invocationContext) throws Exception {

		return process(invocationContext);
	}

	private Object process(InvocationContext invocationContext)
			throws Exception {

		Map<String, Long> values = new HashMap<String, Long>();
		Object target = invocationContext.getTarget();
		LOG.debug("process SNMP on " + target.getClass().getCanonicalName());

		/*
		 * Process the possible SNMP annotation on the method
		 */
		SNMP methodSnmp = invocationContext.getMethod().getAnnotation(
				SNMP.class);
		if (null != methodSnmp) {
			increment(methodSnmp, 1L);
		}

		/*
		 * Process the possible SNMP annotation on the fields
		 */
		Field[] fields = target.getClass().getDeclaredFields();
		for (Field field : fields) {
			SNMP fieldSnmp = field.getAnnotation(SNMP.class);
			if (null == fieldSnmp) {
				continue;
			}

			// retrieve the current value of the associated SNMP counter
			Long value = getValue(fieldSnmp);

			// put this value in the map to compare afterwards
			values.put(field.getName(), value);

			// inject the value into the field
			field.setAccessible(true);
			try {
				field.set(target, value);
			} catch (Exception e) {
				LOG.error("Failed to set field=" + field.getName()
						+ " value to " + value, e);
			}
		}

		/*
		 * Invoke
		 */
		Object result = invocationContext.proceed();

		/*
		 * Post-process the possible SNMP annotation on the fields
		 */
		for (Field field : fields) {
			SNMP fieldSnmp = field.getAnnotation(SNMP.class);
			if (null == fieldSnmp) {
				continue;
			}

			// retrieve the possibly changed field value
			field.setAccessible(true);
			Long value;
			try {
				value = (Long) field.get(target);
			} catch (Exception e) {
				LOG.error("Failed to get field=" + field.getName() + " value",
						e);
				return result;
			}

			// increment the SNMP counter's value with the delta of the current
			// field value and the initial value
			Long increment = value - values.get(field.getName());
			if (increment != 0L)
				increment(fieldSnmp, value - values.get(field.getName()));
		}

		/*
		 * Return the invocation result
		 */
		return result;
	}

	private Long getValue(SNMP snmp) {

		LOG.debug("get value of counter oid=" + snmp.oid() + " @ service="
				+ snmp.service());

		try {
			for (MBeanServer mBeanServer : MBeanServerFactory
					.findMBeanServer(null)) {
				return (Long) mBeanServer.invoke(
						new ObjectName(snmp.service()), "getValue",
						new Object[] { snmp.oid() },
						new String[] { "java.lang.String" });

			}
		} catch (Exception e) {
			LOG.error("Failed to contact SNMP Mbean: " + e.getMessage(), e);
		}

		return 0L;
	}

	private void increment(SNMP snmp, Long increment) {

		LOG.debug("increment counter oid=" + snmp.oid() + " @ service="
				+ snmp.service() + " with " + increment);
		try {
			for (MBeanServer mBeanServer : MBeanServerFactory
					.findMBeanServer(null)) {
				mBeanServer.invoke(new ObjectName(snmp.service()), "increment",
						new Object[] { snmp.oid(), increment }, new String[] {
								"java.lang.String", "java.lang.Long" });

			}
		} catch (Exception e) {
			LOG.error("Failed to contact SNMP Mbean: " + e.getMessage(), e);
		}
	}

}
