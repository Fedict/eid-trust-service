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

package test.unit.be.fedict.trust.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import be.fedict.trust.service.entity.AuditEntity;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.RevokedCertificateEntity;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.TrustPointEntity;
import be.fedict.trust.service.entity.constraints.CertificateConstraintEntity;
import be.fedict.trust.service.entity.constraints.DNConstraintEntity;
import be.fedict.trust.service.entity.constraints.EndEntityConstraintEntity;
import be.fedict.trust.service.entity.constraints.KeyUsageConstraintEntity;
import be.fedict.trust.service.entity.constraints.PolicyConstraintEntity;
import be.fedict.trust.service.entity.constraints.QCStatementsConstraintEntity;

/**
 * JPA entity unit test. Tests the relationships and queries.
 * 
 * @author Frank Cornelis
 * 
 */
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
		configuration.addAnnotatedClass(CertificateConstraintEntity.class);
		configuration.addAnnotatedClass(PolicyConstraintEntity.class);
		configuration.addAnnotatedClass(DNConstraintEntity.class);
		configuration.addAnnotatedClass(EndEntityConstraintEntity.class);
		configuration.addAnnotatedClass(KeyUsageConstraintEntity.class);
		configuration.addAnnotatedClass(QCStatementsConstraintEntity.class);
		configuration.addAnnotatedClass(TrustPointEntity.class);
		configuration.addAnnotatedClass(AuditEntity.class);
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

	private void refresh() {
		// we clear the hibernate cache
		EntityTransaction entityTransaction = this.entityManager
				.getTransaction();
		entityTransaction.commit();
		this.entityManager.clear();
		entityTransaction.begin();
	}

	@Test
	public void testFindRevokedCertificate() throws Exception {
		// setup
		String issuerName = "CN=Test CA";
		BigInteger serialNumber = new BigInteger(
				"21267647932558966653497436382356969621");
		BigInteger crlNumber = new BigInteger("123465789");
		RevokedCertificateEntity revokedCertificateEntity = new RevokedCertificateEntity(
				issuerName, serialNumber, new Date(), crlNumber);
		this.entityManager.persist(revokedCertificateEntity);

		refresh();

		// operate
		Query query = this.entityManager
				.createNamedQuery(RevokedCertificateEntity.QUERY_WHERE_ISSUER_SERIAL);
		query.setParameter("issuer", issuerName);
		query.setParameter("serialNumber", serialNumber.toString());
		RevokedCertificateEntity resultRevokedCertificate = (RevokedCertificateEntity) query
				.getSingleResult();

		// verify
		assertNotNull(resultRevokedCertificate);
		assertEquals(resultRevokedCertificate.getPk().getIssuer(), issuerName);
		assertEquals(resultRevokedCertificate.getPk().getSerialNumber(),
				serialNumber.toString());
		assertEquals(resultRevokedCertificate.getCrlNumber(), crlNumber);

		refresh();

		Query deleteQuery = this.entityManager
				.createNamedQuery(RevokedCertificateEntity.DELETE_WHERE_ISSUER_OLDER_CRL_NUMBER);
		deleteQuery.setParameter("issuer", issuerName);
		deleteQuery.setParameter("crlNumber", crlNumber);
		int zeroDeleteResult = deleteQuery.executeUpdate();
		assertEquals(0, zeroDeleteResult);

		refresh();

		deleteQuery.setParameter("crlNumber",
				crlNumber.add(new BigInteger("1")));
		int deleteResult = deleteQuery.executeUpdate();
		assertEquals(1, deleteResult);
	}
}
