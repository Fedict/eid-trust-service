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

package test.unit.be.fedict.trust.service;

import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.RevokedCertificateEntity;
import be.fedict.trust.service.entity.RevokedCertificatePK;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;

public class PersistenceTest {

	private static final Log LOG = LogFactory.getLog(PersistenceTest.class);

	private EntityManager entityManager;

	@Before
	public void setUp() throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		Ejb3Configuration configuration = new Ejb3Configuration();
		configuration.setProperty("hibernate.dialect",
				"org.hibernate.dialect.HSQLDialect");
		configuration.setProperty("hibernate.connection.driver_class",
				"org.hsqldb.jdbcDriver");
		configuration.setProperty("hibernate.connection.url",
				"jdbc:hsqldb:mem:beta");
		configuration.setProperty("hibernate.hbm2ddl.auto", "create");
		configuration.addAnnotatedClass(CertificateAuthorityEntity.class);
		configuration.addAnnotatedClass(RevokedCertificateEntity.class);
		configuration.addAnnotatedClass(TrustDomainEntity.class);
		configuration.addAnnotatedClass(TrustPointEntity.class);
		EntityManagerFactory entityManagerFactory = configuration
				.buildEntityManagerFactory();

		this.entityManager = entityManagerFactory.createEntityManager();
		this.entityManager.getTransaction().begin();
	}

	@After
	public void tearDown() throws Exception {
		EntityTransaction entityTransaction = this.entityManager
				.getTransaction();
		LOG.debug("entity manager open: " + this.entityManager.isOpen());
		LOG.debug("entity transaction active: " + entityTransaction.isActive());
		if (entityTransaction.isActive()) {
			if (entityTransaction.getRollbackOnly()) {
				entityTransaction.rollback();
			} else {
				entityTransaction.commit();
			}
		}
		this.entityManager.close();
	}

	@Test
	public void testFindRevokedCertificate() throws Exception {
		// setup
		String issuerName = "CN=Test CA";
		BigInteger serialNumber = new BigInteger(
				"21267647932558966653497436382356969621");
		RevokedCertificateEntity revokedCertificateEntity = new RevokedCertificateEntity(
				issuerName, serialNumber, new Date(), BigInteger.valueOf(1234));
		this.entityManager.persist(revokedCertificateEntity);

		// we clear the hibernate cache
		EntityTransaction entityTransaction = this.entityManager
				.getTransaction();
		entityTransaction.commit();
		this.entityManager.clear();
		entityTransaction.begin();

		// operate
		RevokedCertificateEntity resultRevokedCertificateEntity = this.entityManager
				.find(RevokedCertificateEntity.class, new RevokedCertificatePK(
						issuerName, serialNumber));

		// verify
		assertNotNull(resultRevokedCertificateEntity);
	}
}
