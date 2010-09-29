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

    public static final String ACTION = "action";

    public static final String CA_NAME_FIELD = "ca_name_";
    public static final String CA_ROOT_FIELD = "ca_root_";
    public static final String CA_CRL_RECORDS_FIELD = "ca_crl_records_";

    public static final String CA_NAME_LABEL = "CA Name";
    public static final String CA_ROOT_LABEL = "CA Root";
    public static final String CA_CRL_RECORD_LABEL = "# CRL records";

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {

        LOG.debug("doPost");

        String actionString = request.getParameter(ACTION);
        if (null == actionString) {
            throw new ServletException("No " + ACTION + "?!");
        }
        Action action = Action.valueOf(actionString);

        try {
            switch (action) {

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

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<title>PKI Configuration</title>");
        out.println("<body>");

        out.println("<h1>Configuration</h1>");

        // add existing CA's
        for (Map.Entry<String, CAConfiguration> caConfig :
                TestPKI.get().getRootCaConfigurations().entrySet()) {
            addEditRootCAConfig(out, caConfig.getValue());
        }

        out.println("<hr/>");

        // to add new one
        addNewCAConfig(out);

        // generate form
        addGenerate(out);

        out.println("</body>");
    }

    /*
    * Some action helper methods
    */
    private void onAddConfig(HttpServletRequest request) throws Exception {

        LOG.debug("add config");

        String name = request.getParameter(CA_NAME_FIELD);
        String root = request.getParameter(CA_ROOT_FIELD);
        String crlRecordsString = request.getParameter(CA_CRL_RECORDS_FIELD);
        long crlRecords = 0;
        if (!crlRecordsString.isEmpty()) {
            crlRecords = Long.parseLong(crlRecordsString);
        }

        if (null != TestPKI.get().findCa(name)) {
            throw new Exception("CA " + name + " already exists");
        }

        TestPKI.get().addSaveCa(name, root, crlRecords);
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
        long crlRecords = 0;
        if (!crlRecordsString.isEmpty()) {
            crlRecords = Long.parseLong(crlRecordsString);
        }

        TestPKI.get().addSaveCa(name, root, crlRecords);
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

        out.println("<h2>Generate</h2>");
        out.println("<form action=\"" + PATH + "\" method=\"POST\">");

        addSubmit(out, Action.GENERATE);

        out.println("</form>");
    }

    private void addNewCAConfig(PrintWriter out) {

        out.println("<form action=\"" + PATH + "\" method=\"POST\">");

        addTextInput(out, CA_ROOT_LABEL, CA_ROOT_FIELD, "");
        addTextInput(out, CA_NAME_LABEL, CA_NAME_FIELD, "");
        addTextInput(out, CA_CRL_RECORD_LABEL, CA_CRL_RECORDS_FIELD, "");

        addSubmit(out, Action.ADD);

        out.println("</form>");
    }

    private void addEditRootCAConfig(PrintWriter out, CAConfiguration rootCaConfiguration) {

        addEditCAConfig(out, rootCaConfiguration);
        for (CAConfiguration child : rootCaConfiguration.getChilds()) {
            addEditRootCAConfig(out, child);
        }
    }

    private void addEditCAConfig(PrintWriter out, CAConfiguration caConfiguration) {

        out.println("<form action=\"" + PATH + "\" method=\"POST\">");

        addTextInput(out, CA_ROOT_LABEL, CA_ROOT_FIELD + caConfiguration.getName(),
                null != caConfiguration.getRoot() ? caConfiguration.getRoot().getName() : "");
        addDisabledTextInput(out, CA_NAME_LABEL, CA_NAME_FIELD + caConfiguration.getName(), caConfiguration.getName());
        addTextInput(out, CA_CRL_RECORD_LABEL, CA_CRL_RECORDS_FIELD + caConfiguration.getName(),
                Long.toString(caConfiguration.getCrlRecords()));


        addSubmit(out, Action.SAVE);
        addSubmit(out, Action.DELETE);

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

    enum Action {
        SAVE,
        DELETE,
        ADD,
        GENERATE
    }
}
