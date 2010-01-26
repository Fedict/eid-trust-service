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

package be.fedict.trust.service.bean;

import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.BelgianTrustValidatorFactory;
import be.fedict.trust.NetworkConfig;
import be.fedict.trust.TrustLinker;
import be.fedict.trust.TrustValidator;
import be.fedict.trust.service.TrustService;

/**
 * Trust Service Bean implementation.
 * 
 * @author fcorneli
 * 
 */
@Stateless
public class TrustServiceBean implements TrustService {

	private static final Log LOG = LogFactory.getLog(TrustServiceBean.class);

	// TODO: runtime network config via admin configuration console
	// public static final NetworkConfig NETWORK_CONFIG = new NetworkConfig(
	// "proxy.yourict.net", 8080);
	public static final NetworkConfig NETWORK_CONFIG = null;

	private TrustValidator trustValidator;

	@PersistenceContext
	private EntityManager entityManager;

	@Resource(mappedName = "java:JmsXA")
	private QueueConnectionFactory queueConnectionFactory;

	@Resource(mappedName = HarvesterMDB.HARVESTER_QUEUE_NAME)
	private Queue queue;

	@PostConstruct
	public void postConstructCallback() {
		LOG.debug("post construct");
		TrustLinker trustLinker = new TrustServiceTrustLinker(
				this.entityManager, this.queueConnectionFactory, this.queue);
		this.trustValidator = BelgianTrustValidatorFactory
				.createTrustValidator(NETWORK_CONFIG, trustLinker);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean isValid(List<X509Certificate> authenticationCertificateChain) {
		LOG.debug("isValid: "
				+ authenticationCertificateChain.get(0)
						.getSubjectX500Principal());
		try {
			this.trustValidator.isTrusted(authenticationCertificateChain);
		} catch (CertPathValidatorException e) {
			LOG.debug("certificate path validation error: " + e.getMessage());
			return false;
		}
		return true;
	}
}
