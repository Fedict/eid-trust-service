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

package be.fedict.trust.service.bean;

import be.fedict.trust.crl.CrlTrustLinker;
import be.fedict.trust.crl.OnlineCrlRepository;
import be.fedict.trust.service.SnmpConstants;
import be.fedict.trust.service.dao.AuditDAO;
import be.fedict.trust.service.dao.CertificateAuthorityDAO;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.Status;
import be.fedict.trust.service.snmp.SNMP;
import be.fedict.trust.service.snmp.SNMPInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.interceptor.Interceptors;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = HarvesterMDB.HARVESTER_QUEUE_NAME)})
@TransactionManagement(value = TransactionManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
@Interceptors(SNMPInterceptor.class)
public class HarvesterMDB implements MessageListener {

    private static final Log LOG = LogFactory.getLog(HarvesterMDB.class);

    public static final String HARVESTER_QUEUE_NAME = "queue/trust/harvester";

    private static final int BATCH_SIZE = 10000;

    @EJB
    private ConfigurationDAO configurationDAO;

    @EJB
    private CertificateAuthorityDAO certificateAuthorityDAO;

    @EJB
    private AuditDAO auditDAO;

    @SNMP(oid = SnmpConstants.CRL_DOWNLOAD_FAILURES)
    private Long failures = 0L;

    @PostConstruct
    public void postConstructCallback() {
        LOG.debug("post construct");
    }

    @SNMP(oid = SnmpConstants.CACHE_REFRESH)
    public void onMessage(Message message) {
        LOG.debug("onMessage");
        HarvestMessage harvestMessage;
        try {
            harvestMessage = new HarvestMessage(message);
        } catch (JMSException e) {
            LOG.error("JMS error: " + e.getMessage());
            return;
        }
        String caName = harvestMessage.getCaName();
        boolean update = harvestMessage.isUpdate();

        LOG.debug("issuer: " + caName);
        CertificateAuthorityEntity certificateAuthority = this.certificateAuthorityDAO
                .findCertificateAuthority(caName);
        if (null == certificateAuthority) {
            LOG.warn("unknown certificate authority: " + caName);
            return;
        }
        if (!update && Status.PROCESSING != certificateAuthority.getStatus()) {
            /*
                * Possible that another harvester instance already activated or is
                * processing the CA cache in the meanwhile.
                */
            LOG.debug("CA status not marked for processing");
            return;
        }
        String crlUrl = certificateAuthority.getCrlUrl();
        if (null == crlUrl) {
            LOG.warn("No CRL url for CA " + certificateAuthority.getName());
            certificateAuthority.setStatus(Status.NONE);
            return;
        }

        OnlineCrlRepository onlineCrlRepository = new OnlineCrlRepository(
                this.configurationDAO.getNetworkConfig());
        URI crlUri;
        try {
            crlUri = new URI(crlUrl);
        } catch (URISyntaxException e) {
            LOG.error("CRL URI error: " + e.getMessage(), e);
            return;
        }
        Date validationDate = new Date();
        X509CRL crl = onlineCrlRepository.findCrl(crlUri, certificateAuthority
                .getCertificate(), validationDate);
        if (null == crl) {
            this.auditDAO.logAudit("Failed to download CRL for CA=" + caName
                    + " @ " + crlUrl);
            this.failures++;
            throw new RuntimeException();
        }

        X509Certificate issuerCertificate = certificateAuthority
                .getCertificate();

        LOG.debug("checking integrity CRL...");
        boolean crlValid = CrlTrustLinker.checkCrlIntegrity(crl,
                issuerCertificate, validationDate);
        if (!crlValid) {
            this.auditDAO.logAudit("Invalid CRL for CA=" + caName + " @ "
                    + crlUrl);
            return;
        }
        LOG.debug("processing CRL... " + caName);
        BigInteger crlNumber = getCrlNumber(crl);
        LOG.debug("CRL number: " + crlNumber);

        Set<? extends X509CRLEntry> revokedCertificates = crl
                .getRevokedCertificates();
        if (null != revokedCertificates) {
            LOG.debug("found " + revokedCertificates.size() + " crl entries");

            long entriesFound = 0;
            if (null != crlNumber) {
                entriesFound = this.certificateAuthorityDAO
                        .countRevokedCertificates(crlNumber, crl
                                .getIssuerX500Principal().toString());
            }

            if (entriesFound > 0) {
                LOG.debug("entries already added, skipping... (#="
                        + entriesFound + ")");
            } else {
                LOG.debug("no entries found, adding... ( batchSize="
                        + BATCH_SIZE + ")");

                /*
                     * Split up persisting the crl entries to avoid memory issues.
                     */
                Set<X509CRLEntry> revokedCertsBatch = new HashSet<X509CRLEntry>();
                int added = 0;
                for (X509CRLEntry revokedCertificate : crl
                        .getRevokedCertificates()) {
                    revokedCertsBatch.add(revokedCertificate);
                    added++;
                    if (added == BATCH_SIZE) {
                        /*
                               * Persist batch
                               */
                        this.certificateAuthorityDAO.addRevokedCertificates(
                                revokedCertsBatch, crlNumber, crl
                                        .getIssuerX500Principal());
                        revokedCertsBatch.clear();
                        added = 0;
                    }
                }
                /*
                     * Persist final batch
                     */
                this.certificateAuthorityDAO.addRevokedCertificates(
                        revokedCertsBatch, crlNumber, crl
                                .getIssuerX500Principal());

                /*
                     * Cleanup redundant CRL entries
                     */
                if (null != crlNumber) {
                    this.certificateAuthorityDAO.removeOldRevokedCertificates(
                            crlNumber, crl.getIssuerX500Principal().toString());
                }

            }
        }

        LOG.debug("CRL this update: " + crl.getThisUpdate());
        LOG.debug("CRL next update: " + crl.getNextUpdate());
        certificateAuthority.setStatus(Status.ACTIVE);
        certificateAuthority.setThisUpdate(crl.getThisUpdate());
        certificateAuthority.setNextUpdate(crl.getNextUpdate());
        LOG.debug("cache activated for CA: " + crl.getIssuerX500Principal());
    }

    private BigInteger getCrlNumber(X509CRL crl) {
        byte[] crlNumberExtensionValue = crl.getExtensionValue("2.5.29.20");
        if (null == crlNumberExtensionValue) {
            return null;
        }
        try {
            DEROctetString octetString = (DEROctetString) (new ASN1InputStream(
                    new ByteArrayInputStream(crlNumberExtensionValue))
                    .readObject());
            byte[] octets = octetString.getOctets();
            DERInteger integer = (DERInteger) new ASN1InputStream(octets)
                    .readObject();
            return integer.getPositiveValue();
        } catch (IOException e) {
            throw new RuntimeException("IO error: " + e.getMessage(), e);
        }
    }
}
