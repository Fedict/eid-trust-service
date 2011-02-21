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

package be.fedict.trust.service.dao.bean;

import be.fedict.trust.service.dao.AdministratorDAO;
import be.fedict.trust.service.entity.AdministratorEntity;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Administrator DAO Bean implementation.
 *
 * @author wvdhaute
 */
@Stateless
public class AdministratorDAOBean implements AdministratorDAO {

    private static final Log LOG = LogFactory
            .getLog(AdministratorDAOBean.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<AdministratorEntity> listAdmins() {

        LOG.debug("list admins");
        Query query = this.entityManager
                .createNamedQuery(AdministratorEntity.QUERY_LIST_ALL);
        return (List<AdministratorEntity>) query.getResultList();
    }

    /**
     * {@inheritDoc}
     */
    public AdministratorEntity findAdmin(X509Certificate certificate) {

        LOG.debug("find admin");
        String id = getId(certificate);
        return this.entityManager.find(AdministratorEntity.class, id);
    }

    /**
     * {@inheritDoc}
     */
    public AdministratorEntity addAdmin(X509Certificate authnCertificate,
                                        boolean pending) {

        LOG.debug("add admin pending=" + pending);
        String name = authnCertificate.getSubjectX500Principal().toString();
        AdministratorEntity admin = new AdministratorEntity(
                getId(authnCertificate), name, pending);
        this.entityManager.persist(admin);
        return admin;
    }

    /**
     * {@inheritDoc}
     */
    public void removeAdmin(AdministratorEntity admin) {

        LOG.debug("remove admin: " + admin.getName());
        AdministratorEntity attachedAdmin = this.entityManager.find(
                AdministratorEntity.class, admin.getId());
        this.entityManager.remove(attachedAdmin);
    }

    /**
     * {@inheritDoc}
     */
    public AdministratorEntity attachAdmin(AdministratorEntity admin) {

        return this.entityManager.find(AdministratorEntity.class, admin.getId());
    }

    private String getId(X509Certificate certificate) {
        PublicKey publicKey = certificate.getPublicKey();
        return DigestUtils.shaHex(publicKey.getEncoded());
    }
}
