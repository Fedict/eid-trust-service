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
import java.lang.reflect.Method;
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
 * In case an {@link SNMP} annotation is put on the method, each invocation will
 * trigger an increment of 1 on the value associated with the configured SNMP
 * OID.
 * 
 * In case an {@link SNMP} annotation is put on a field, before an invocation in
 * one of the classes methods, the current value associated with the SNMP OID
 * will be fetched and injected in the field. After completion of the method,
 * the updated value will be checked and, if changed the, delta will be
 * incremented in the SNMP value. In case the field is derived, nothing will be
 * updated here, the update happens in a method annotated with
 * {@link SNMPCounter}.
 * 
 * In case an {@link SNMPCounter} annotation is put on a field, after every
 * invocation of other methods, the interceptor will execute this method. This
 * is helpful for derived {@link SNMP} fields, dependent on other {@link SNMP}
 * fields to limit faults due to changes while invoking.
 * 
 * @author wvdhaute
 * 
 */
public class SNMPInterceptor {

	private static final Log LOG = LogFactory.getLog(SNMPInterceptor.class);

	private Map<String, Long> values;

	@AroundInvoke
	public Object invoke(InvocationContext invocationContext) throws Exception {
		return process(invocationContext);
	}

	private Object process(InvocationContext invocationContext)
			throws Exception {

		this.values = new HashMap<String, Long>();
		Object target = invocationContext.getTarget();
		LOG.debug("process SNMP on " + target.getClass().getCanonicalName());

		/*
		 * Process the possible SNMP annotation on the method
		 */
		SNMP methodSnmp = invocationContext.getMethod().getAnnotation(
				SNMP.class);
		if (null != methodSnmp) {
			increment(methodSnmp.oid(), methodSnmp.service(), 1L);
		}

		/*
		 * Process the possible SNMP annotation on the fields
		 */
		injectSnmpFields(target);

		/*
		 * Invoke
		 */
		Object result = invocationContext.proceed();

		/*
		 * Post-process the possible SNMP annotation on the fields
		 */
		updateSnmpFields(target);

		/*
		 * Check for SNMPCounter methods
		 */
		SNMPCounter snmpCounter = invocationContext.getMethod().getAnnotation(
				SNMPCounter.class);
		if (null == snmpCounter) {
			// check if other methods are annotated this way, if so execute them
			for (Method method : target.getClass().getMethods()) {
				if (null != method.getAnnotation(SNMPCounter.class)) {
					method.invoke(target);
				}
			}
		}

		/*
		 * Update the SNMP derived fields
		 */
		updateSnmpDerivedFields(target);

		/*
		 * Return the invocation result
		 */
		return result;
	}

	private void injectSnmpFields(Object target) {

		for (Field field : target.getClass().getDeclaredFields()) {
			SNMP fieldSnmp = field.getAnnotation(SNMP.class);
			if (null == fieldSnmp) {
				continue;
			}

			// retrieve the current value of the associated SNMP counter
			Long value = getValue(fieldSnmp.oid(), fieldSnmp.service());

			// put this value in the map to compare afterwards
			this.values.put(field.getName(), value);

			// inject the value into the field
			field.setAccessible(true);
			try {
				field.set(target, value);
			} catch (Exception e) {
				LOG.error("Failed to set field=" + field.getName()
						+ " value to " + value, e);
			}
		}
	}

	private void updateSnmpFields(Object target) {

		for (Field field : target.getClass().getDeclaredFields()) {
			SNMP fieldSnmp = field.getAnnotation(SNMP.class);
			/*
			 * Derived SNMP fields are updated on SNMPCounter method invocations
			 */
			if (null == fieldSnmp || fieldSnmp.derived()) {
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
				return;
			}

			// increment the SNMP counter's value with the delta of the current
			// field value and the initial value
			Long increment = value - this.values.get(field.getName());
			if (increment != 0L)
				increment(fieldSnmp.oid(), fieldSnmp.service(), value
						- this.values.get(field.getName()));
		}
	}

	private void updateSnmpDerivedFields(Object target) {

		for (Field field : target.getClass().getDeclaredFields()) {
			SNMP fieldSnmp = field.getAnnotation(SNMP.class);
			if (null == fieldSnmp || !fieldSnmp.derived()) {
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
				return;
			}

			// set the SNMP value
			setValue(fieldSnmp.oid(), fieldSnmp.service(), value);
		}

	}

	private static Long getValue(String oid, String service) {

		LOG.debug("get value of counter oid=" + oid + " @ service=" + service);

		try {
			for (MBeanServer mBeanServer : MBeanServerFactory
					.findMBeanServer(null)) {
				return (Long) mBeanServer.invoke(new ObjectName(service),
						"getValue", new Object[] { oid },
						new String[] { "java.lang.String" });

			}
		} catch (Exception e) {
			LOG.error("Failed to contact SNMP Mbean: " + e.getMessage(), e);
		}

		return 0L;
	}

	public static void setValue(String oid, String service, Long value) {

		LOG.debug("set value of counter oid=" + oid + " @ service=" + service
				+ " to " + value);

		try {
			for (MBeanServer mBeanServer : MBeanServerFactory
					.findMBeanServer(null)) {
				mBeanServer.invoke(new ObjectName(service), "setValue",
						new Object[] { oid, value }, new String[] {
								"java.lang.String", "java.lang.Long" });

			}
		} catch (Exception e) {
			LOG.error("Failed to contact SNMP Mbean: " + e.getMessage(), e);
		}
	}

	public static void increment(String oid, String service, Long increment) {

		LOG.debug("increment counter oid=" + oid + " @ service=" + service
				+ " with " + increment);

		try {
			for (MBeanServer mBeanServer : MBeanServerFactory
					.findMBeanServer(null)) {
				mBeanServer.invoke(new ObjectName(service), "increment",
						new Object[] { oid, increment }, new String[] {
								"java.lang.String", "java.lang.Long" });

			}
		} catch (Exception e) {
			LOG.error("Failed to contact SNMP Mbean: " + e.getMessage(), e);
		}

	}

}
