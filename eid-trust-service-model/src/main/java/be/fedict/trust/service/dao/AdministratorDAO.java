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

package be.fedict.trust.service.dao;

import be.fedict.trust.service.entity.AdministratorEntity;

import javax.ejb.Local;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Administrator DAO.
 *
 * @author wvdhaute
 */
@Local
public interface AdministratorDAO {

    /**
     * Returns list of registered administrators.
     */
    List<AdministratorEntity> listAdmins();

    /**
     * Returns {@link AdministratorEntity} matching the specified authentication
     * {@link X509Certificate}, or <code>null</code> if not found.
     *
     * @param certificate admin's authentication certificate
     */
    AdministratorEntity findAdmin(X509Certificate certificate);

    /**
     * Add new {@link be.fedict.trust.service.entity.AdministratorEntity}
     *
     * @param authnCertificate
     * @param pending          pending dministrator or not
     */
    AdministratorEntity addAdmin(X509Certificate authnCertificate, boolean pending);

    /**
     * Remove the specified {@link be.fedict.trust.service.entity.AdministratorEntity}.
     *
     * @param admin
     */
    void removeAdmin(AdministratorEntity admin);

    /**
     * Return the attached {@link be.fedict.trust.service.entity.AdministratorEntity}.
     *
     * @param admin
     */
    AdministratorEntity attachAdmin(AdministratorEntity admin);
}
