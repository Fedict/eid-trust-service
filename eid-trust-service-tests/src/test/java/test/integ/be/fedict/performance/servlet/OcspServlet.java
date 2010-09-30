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

package test.integ.be.fedict.performance.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.ocsp.RevokedInfo;
import org.bouncycastle.ocsp.*;
import test.integ.be.fedict.performance.CAConfiguration;
import test.integ.be.fedict.performance.TestPKI;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;

public class OcspServlet extends HttpServlet {

    private static final Log LOG = LogFactory.getLog(OcspServlet.class);

    public static final String PATH = "ocsp";

    public static final String CA_QUERY_PARAM = "ca";

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {

        String caName = request.getParameter(CA_QUERY_PARAM);
        if (null == caName) {
            throw new ServletException("No CA name found.");
        }

        CAConfiguration ca = TestPKI.get().findCa(caName);
        if (null == ca) {
            throw new ServletException("CA Config not found for " + caName);
        }

        try {

            if (null == request.getContentType()
                    || !request.getContentType().equals(
                    "application/ocsp-request")) {
                LOG.error("Wrong content type: " + request.getContentType());
                return;
            }

            int len = request.getContentLength();
            byte[] ocspRequestData = new byte[len];
            ServletInputStream reader = request.getInputStream();
            int result = reader.read(ocspRequestData);
            if (-1 == result) {
                LOG.error("No data found ?!");
                return;
            }

            BigInteger maxRevokedSN = new BigInteger(Long.toString(ca.getCrlRecords()));

            OCSPReq ocspRequest = new OCSPReq(ocspRequestData);

            BasicOCSPRespGenerator ocspRespGen = new BasicOCSPRespGenerator(
                    ca.getKeyPair().getPublic());
            for (Req req : ocspRequest.getRequestList()) {

                LOG.debug("OCSP request for CA=" + caName
                        + " sn=" + req.getCertID().getSerialNumber());

                if (-1 == req.getCertID().getSerialNumber().compareTo(maxRevokedSN)) {
                    LOG.debug("revoked");
                    ocspRespGen.addResponse(req.getCertID(), new RevokedStatus(
                            new RevokedInfo(new DERGeneralizedTime(new Date()),
                                    null)));
                } else {
                    ocspRespGen.addResponse(req.getCertID(),
                            CertificateStatus.GOOD);
                }
            }

            BasicOCSPResp ocspResp;
            ocspResp = ocspRespGen.generate("SHA1WITHRSA",
                    ca.getKeyPair().getPrivate(), null, new Date(), "BC");

            OCSPRespGenerator og = new OCSPRespGenerator();

            response.setContentType("application/ocsp-response");
            response.getOutputStream().write(
                    (og.generate(OCSPRespGenerator.SUCCESSFUL, ocspResp))
                            .getEncoded());

        } catch (Exception e) {
            LOG.error("Exception: " + e.getMessage(), e);
            throw new ServletException(e);
        }
    }

    public static String getPath(String caName) {
        return TestPKI.get().getPath() + "/" + PATH + "?" + CA_QUERY_PARAM + "=" + caName;
    }
}
