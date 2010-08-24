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

package be.fedict.trust.service.entity;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

@Entity
@Table(name = "ts_revoked_certs")
@NamedQueries({
        @NamedQuery(name = RevokedCertificateEntity.QUERY_WHERE_ISSUER_SERIAL, query = "FROM RevokedCertificateEntity "
                + "WHERE issuer = :issuer AND serialNumber = :serialNumber ORDER BY crlNumber DESC"),
        @NamedQuery(name = RevokedCertificateEntity.QUERY_COUNT_WHERE_ISSUER_CRL_NUMBER, query = "SELECT COUNT(*) "
                + "FROM RevokedCertificateEntity WHERE issuer = :issuer AND crlNumber = :crlNumber"),
        @NamedQuery(name = RevokedCertificateEntity.DELETE_WHERE_ISSUER_OLDER_CRL_NUMBER, query = "DELETE FROM RevokedCertificateEntity "
                + "WHERE crlNumber < :crlNumber AND issuer = :issuer")})
public class RevokedCertificateEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String QUERY_WHERE_ISSUER_SERIAL = "ts_rc.q.i.s";
    public static final String QUERY_COUNT_WHERE_ISSUER_CRL_NUMBER = "ts_rc.q.c.i.c";
    public static final String DELETE_WHERE_ISSUER_OLDER_CRL_NUMBER = "ts_rc.d.i.old.c";

    private long id;

    private String issuer;
    private BigInteger serialNumber;
    private Date revocationDate;
    private BigInteger crlNumber;

    public RevokedCertificateEntity() {

        super();
    }

    public RevokedCertificateEntity(String issuer, BigInteger serialNumber,
                                    Date revocationDate, BigInteger crlNumber) {

        this.issuer = issuer;
        this.serialNumber = serialNumber;
        this.revocationDate = revocationDate;
        this.crlNumber = crlNumber;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long getId() {

        return this.id;
    }

    public void setId(long id) {

        this.id = id;
    }

    public String getIssuer() {

        return this.issuer;
    }

    public void setIssuer(String issuer) {

        this.issuer = issuer;
    }

    @Column(precision = 38)
    public BigInteger getSerialNumber() {

        return this.serialNumber;
    }

    public void setSerialNumber(BigInteger serialNumber) {

        this.serialNumber = serialNumber;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getRevocationDate() {
        return this.revocationDate;
    }

    public void setRevocationDate(Date revocationDate) {
        this.revocationDate = revocationDate;
    }

    @Column(precision = 38)
    public BigInteger getCrlNumber() {
        return this.crlNumber;
    }

    public void setCrlNumber(BigInteger crlNumber) {
        this.crlNumber = crlNumber;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (null == obj) {
            return false;
        }
        if (false == obj instanceof RevokedCertificateEntity) {
            return false;
        }
        RevokedCertificateEntity rhs = (RevokedCertificateEntity) obj;
        return new EqualsBuilder().append(this.id, rhs.id).isEquals();
    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder().append(this.id).toHashCode();
    }

    @Override
    public String toString() {

        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", this.id).append("issuer", this.issuer).append(
                        "serialNumber", this.serialNumber).append("crlNumber",
                        this.crlNumber).append("revocationDate",
                        this.revocationDate)
                .append("crlNumber", this.crlNumber).toString();
    }
}
