/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2012 FedICT.
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

import java.math.BigInteger;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.Date;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.service.ValidationService;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.RevokedCertificateEntity;
import be.fedict.trust.service.entity.RevokedCertificatePK;
import be.fedict.trust.service.entity.Status;

/**
 * Validation Service implementation.
 * 
 * @author Frank Cornelis
 */
@Stateless
public class ValidationServiceBean implements ValidationService {

	private static final Log LOG = LogFactory
			.getLog(ValidationServiceBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	@EJB
	private CertificateAuthorityLookupBean certificateAuthorityLookupBean;

	@EJB
	private ServiceIdentityManagerBean serviceIdentityManagerBean;

	public boolean validate(BigInteger serialNumber, byte[] issuerNameHash,
			byte[] issuerKeyHash) {
		LOG.debug("validate");
		CertificateAuthorityEntity certificateAuthority = this.certificateAuthorityLookupBean
				.lookup(issuerNameHash, issuerKeyHash);
		if (null == certificateAuthority) {
			LOG.error("no certificate authority found");
			return false;
		}
		String caName = certificateAuthority.getName();
		LOG.debug("CA: " + caName);
		Date thisUpdate = certificateAuthority.getThisUpdate();
		Date nextUpdate = certificateAuthority.getNextUpdate();
		Date validationDate = new Date();
		if (Status.ACTIVE != certificateAuthority.getStatus()) {
			LOG.debug("CRL cache not active for CA: " + caName);
			return false;
		}
		if (null == thisUpdate || validationDate.before(thisUpdate)) {
			LOG.debug("validation date before this update: " + caName);
			return false;
		}
		if (null == nextUpdate || validationDate.after(nextUpdate)) {
			LOG.debug("validation date after next update: " + caName);
			return false;
		}
		RevokedCertificateEntity revokedCertificate = this.entityManager.find(
				RevokedCertificateEntity.class, new RevokedCertificatePK(
						caName, serialNumber.toString()));
		if (null == revokedCertificate) {
			return true;
		}
		LOG.debug("revoked certificate: " + caName + " " + serialNumber);
		return false;
	}

	public PrivateKeyEntry getPrivateKeyEntry() {
		return this.serviceIdentityManagerBean.getPrivateKeyEntry();
	}
}
