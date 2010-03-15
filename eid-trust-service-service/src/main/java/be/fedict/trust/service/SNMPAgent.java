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

package be.fedict.trust.service;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.io.ImportModes;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.ThreadPool;

public class SNMPAgent extends BaseAgent {

	private List<Counter> counters;

	public SNMPAgent(File bootCounterFile, File configFile) throws IOException {

		super(bootCounterFile, configFile, new CommandProcessor(
				new OctetString(MPv3.createLocalEngineID())));
		agent.setWorkerPool(ThreadPool.create("RequestPool", 4));
		this.counters = new LinkedList<Counter>();
		init();
		loadConfig(ImportModes.REPLACE_CREATE);
		addShutdownHook();
		getServer().addContext(new OctetString("public"));
		finishInit();
	}

	@Override
	protected void initTransportMappings() throws IOException {

		transportMappings = new TransportMapping[1];
		transportMappings[0] = new DefaultUdpTransportMapping(new UdpAddress(
				"0.0.0.0/7894"));
	}

	@Override
	protected void registerManagedObjects() {
	}

	@Override
	protected void unregisterManagedObjects() {

		for (Counter counter : this.counters) {
			getServer().unregister(counter, getDefaultContext());
		}
	}

	/**
	 * Register a new {@link Counter}.
	 * 
	 * @param counter
	 * @throws DuplicateRegistrationException
	 */
	public void registerCounter(Counter counter)
			throws DuplicateRegistrationException {
		this.counters.add(counter);
		getServer().register(counter, getDefaultContext());
	}

	@Override
	protected void addCommunities(SnmpCommunityMIB communityMIB) {

		Variable[] com2sec = new Variable[] { new OctetString("public"), // community
				// name
				new OctetString("cpublic"), // security name
				getAgent().getContextEngineID(), // local engine ID
				new OctetString("public"), // default context name
				new OctetString(), // transport tag
				new Integer32(StorageType.nonVolatile), // storage type
				new Integer32(RowStatus.active) // row status
		};
		MOTableRow row = communityMIB.getSnmpCommunityEntry().createRow(
				new OctetString("public2public").toSubIndex(true), com2sec);
		communityMIB.getSnmpCommunityEntry().addRow(row);
	}

	@Override
	protected void addNotificationTargets(SnmpTargetMIB targetMIB,
			SnmpNotificationMIB notificationMIB) {

		targetMIB.addDefaultTDomains();
	}

	@Override
	protected void addUsmUser(USM usm) {

		// User-based security model :
		// http://www.apps.ietf.org/rfc/rfc2574.html

	}

	@Override
	protected void addViews(VacmMIB vacm) {

		// View-based Access Control Model :
		// http://www.apps.ietf.org/rfc/rfc3415.html

		// add groups
		vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv1, new OctetString(
				"cpublic"), new OctetString("v1v2group"),
				StorageType.nonVolatile);
		vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(
				"cpublic"), new OctetString("v1v2group"),
				StorageType.nonVolatile);

		// add access to groups ( empty octet string means no access )
		vacm.addAccess(new OctetString("v1v2group"), new OctetString("public"),
				SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV,
				MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
				new OctetString(""), new OctetString(""),
				StorageType.nonVolatile);

		// add view families
		vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"),
				new OctetString(), VacmMIB.vacmViewIncluded,
				StorageType.nonVolatile);
	}
}
