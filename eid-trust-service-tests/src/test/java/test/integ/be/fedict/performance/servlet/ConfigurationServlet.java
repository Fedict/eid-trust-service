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
import test.integ.be.fedict.performance.CAConfiguration;
import test.integ.be.fedict.performance.TestPKI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;

public class ConfigurationServlet extends HttpServlet {

    private static final Log LOG = LogFactory.getLog(ConfigurationServlet.class);

    public static final String PATH = "configuration";

    public static final String FIELD_SEPERATOR = ";";

    private static final String CA_NAME_FIELD = "ca_name_";
    private static final String CA_ROOT_FIELD = "ca_root_";
    private static final String CA_CRL_RECORDS_FIELD = "ca_crl_records_";
    private static final String CA_CRL_REFRESH_FIELD = "ca_crl_refresh";

    private static final String CA_NAME_LABEL = "CA Name";
    private static final String CA_ROOT_LABEL = "CA Root";
    private static final String CA_CRL_RECORD_LABEL = "# CRL records";
    private static final String CA_CRL_REFRESH_LABEL = "CRL refresh (mins)";

    public static final String ACTION = "action";

    public enum Action {
        SAVE,
        DELETE,
        ADD,
        GET,
        GENERATE
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {

        LOG.debug("doPost");

        Action action = getAction(request);
        if (null == action) {
            throw new ServletException("No action :(");
        }

        try {
            switch (action) {

                case GET:
                    outputConfig(response);
                    return;
                case SAVE:
                    onSaveConfig(request);
                    break;
                case DELETE:
                    onDeleteConfig(request);
                    break;
                case ADD:
                    onAddConfig(request);
                    break;
                case GENERATE:
                    TestPKI.get().generate();
                    break;
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }

        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        Action action = getAction(request);
        if (null != action && action.equals(Action.GET)) {
            outputConfig(response);
            return;
        }


        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<title>PKI Configuration</title>");
        out.println("<body>");

        out.println("<h1>Configuration</h1>");

        out.println("<hr/>");

        // add existing CA's
        for (Map.Entry<String, CAConfiguration> caConfig :
                TestPKI.get().getRoots().entrySet()) {
            addEditRootCAMarkup(out, caConfig.getValue());
        }

        out.println("<hr/>");

        // to add new one
        addNewCAMarkup(out);

        out.println("<hr/>");

        // generate form
        addGenerate(out);

        out.println("</body>");
    }

    private Action getAction(HttpServletRequest request) {

        String actionString = request.getParameter(ACTION);
        if (null == actionString) {
            return null;
        }
        return Action.valueOf(actionString);

    }

    /*
    * Some action helper methods
    */
    private void outputConfig(HttpServletResponse response) throws IOException {

        LOG.debug("output config");
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        for (CAConfiguration rootCa : TestPKI.get().getRoots().values()) {
            outputParentCa(out, rootCa);
        }
    }

    private void outputParentCa(PrintWriter out, CAConfiguration parentCa) {

        outputCa(out, parentCa);
        for (CAConfiguration child : parentCa.getChilds()) {
            outputParentCa(out, child);
        }
    }

    private void outputCa(PrintWriter out, CAConfiguration ca) {
        out.print(ca.getName());
        out.print(FIELD_SEPERATOR);
        out.print((null == ca.getRoot() ? "" : ca.getRoot().getName()));
        out.print(FIELD_SEPERATOR);
        out.print(ca.getCrlRecords());
        out.print(FIELD_SEPERATOR);
        out.print(ca.getCrlRefresh());
        out.println();
    }

    private void onAddConfig(HttpServletRequest request) throws Exception {

        LOG.debug("add config");

        String name = request.getParameter(CA_NAME_FIELD);
        String root = request.getParameter(CA_ROOT_FIELD);
        String crlRecordsString = request.getParameter(CA_CRL_RECORDS_FIELD);
        String crlRefreshString = request.getParameter(CA_CRL_REFRESH_FIELD);
        long crlRecords = 0;
        if (!crlRecordsString.isEmpty()) {
            crlRecords = Long.parseLong(crlRecordsString);
        }
        int crlRefresh = 0;
        if (!crlRefreshString.isEmpty()) {
            crlRefresh = Integer.parseInt(crlRefreshString);
        }

        if (null != TestPKI.get().findCa(name)) {
            throw new Exception("CA " + name + " already exists");
        }

        TestPKI.get().addSaveCa(name, root, crlRecords, crlRefresh);
    }

    private void onDeleteConfig(HttpServletRequest request) throws Exception {

        String name = getName(request);
        TestPKI.get().removeCa(name);
    }

    private void onSaveConfig(HttpServletRequest request) throws Exception {

        LOG.debug("save config");

        String name = getName(request);
        String root = request.getParameter(CA_ROOT_FIELD + name);
        String crlRecordsString = request.getParameter(CA_CRL_RECORDS_FIELD + name);
        String crlRefreshString = request.getParameter(CA_CRL_REFRESH_FIELD + name);
        long crlRecords = 0;
        if (!crlRecordsString.isEmpty()) {
            crlRecords = Long.parseLong(crlRecordsString);
        }
        int crlRefresh = 0;
        if (!crlRefreshString.isEmpty()) {
            crlRefresh = Integer.parseInt(crlRefreshString);
        }

        TestPKI.get().addSaveCa(name, root, crlRecords, crlRefresh);
    }

    private String getName(HttpServletRequest request) throws ServletException {

        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            if (paramName.equals(CA_NAME_FIELD)) {
                return request.getParameter(paramName);
            } else if (!paramName.equals(CA_ROOT_FIELD) && paramName.startsWith(CA_ROOT_FIELD)) {
                return paramName.substring(paramName.indexOf(CA_ROOT_FIELD) + CA_ROOT_FIELD.length());
            }
        }

        throw new ServletException("No CA name found?");
    }

    /*
     * Some HTML generation helper methods
     */

    private void addGenerate(PrintWriter out) {

        out.println("<form action=\"" + PATH + "\" method=\"POST\">");

        addSubmit(out, Action.GENERATE);

        out.println("</form>");
    }

    private void addNewCAMarkup(PrintWriter out) {

        out.println("<form action=\"" + PATH + "\" method=\"POST\">");

        addTextInput(out, CA_ROOT_LABEL, CA_ROOT_FIELD, "");
        addTextInput(out, CA_NAME_LABEL, CA_NAME_FIELD, "");
        addTextInput(out, CA_CRL_RECORD_LABEL, CA_CRL_RECORDS_FIELD, "");
        addTextInput(out, CA_CRL_REFRESH_LABEL, CA_CRL_REFRESH_FIELD, "");

        addSubmit(out, Action.ADD);

        out.println("</form>");
    }

    private void addEditRootCAMarkup(PrintWriter out, CAConfiguration rootCa) {

        addEditCAMarkup(out, rootCa);
        for (CAConfiguration child : rootCa.getChilds()) {
            addEditRootCAMarkup(out, child);
        }
    }

    private void addEditCAMarkup(PrintWriter out, CAConfiguration ca) {

        out.println("<form action=\"" + PATH + "\" method=\"POST\">");

        addTextInput(out, CA_ROOT_LABEL, CA_ROOT_FIELD + ca.getName(),
                null != ca.getRoot() ? ca.getRoot().getName() : "");
        addDisabledTextInput(out, CA_NAME_LABEL, CA_NAME_FIELD + ca.getName(), ca.getName());
        addTextInput(out, CA_CRL_RECORD_LABEL, CA_CRL_RECORDS_FIELD + ca.getName(),
                Long.toString(ca.getCrlRecords()));
        addTextInput(out, CA_CRL_REFRESH_LABEL, CA_CRL_REFRESH_FIELD + ca.getName(),
                Integer.toString(ca.getCrlRefresh()));


        addSubmit(out, Action.SAVE);
        addSubmit(out, Action.DELETE);

        if (null != ca.getCertificate()) {
            addLink(out, CertificateServlet.getPath(ca.getName()), "Certificate");
        }

        out.println("</form>");
    }

    private void addTextInput(PrintWriter out, String label, String name, String value) {

        out.print(label + "&nbsp; &nbsp;");
        out.println("<input type=\"text\" name=\"" + name + "\" value=\"" + value + "\" />");
    }

    private void addDisabledTextInput(PrintWriter out, String label, String name, String value) {

        out.print(label + "&nbsp; &nbsp;");
        out.println("<input type=\"text\" name=\"" + name + "\" value=\"" + value + "\" disabled=\"true\" />");
    }

    private void addSubmit(PrintWriter out, Action action) {
        out.println("<input name=\"" + ACTION + "\" type=\"submit\" value=\"" + action + "\"/>");
    }

    private void addLink(PrintWriter out, String path, String value) {
        out.println("<a href=\"" + path + "\">" + value + "</a>");
    }
}
