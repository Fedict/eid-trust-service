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

package test.integ.be.fedict.performance;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.openssl.PEMReader;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.LocalConnector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.servlet.*;
import test.integ.be.fedict.performance.servlet.*;

import javax.servlet.http.HttpServlet;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Test PKI which can be used by the trust service.
 * <p/>
 * {@link #start(String)} will start a jetty servlet server which publishes:
 * <ul>
 * <li>{@link ConfigurationServlet} to view/edit the PKI setup
 * <li>{@link CrlServlet} to download the CRL for specified CA
 * <li>{@link OcspServlet} for OCSP request for specified CA
 * <li>{@link CertificateServlet} to download the specified CA's certificate
 * <li>{@link PrivateKeyServlet} to download the specified CA's private key in PEM format.
 * </ul>
 */
public class TestPKI {

    private static final Log LOG = LogFactory.getLog(TestPKI.class);

    private static TestPKI testPKI;

    // Jetty configuration
    private String host;
    private Server server;
    private String path;
    private List<String> servletPaths;

    // PKI configuration
    private Map<String, CAConfiguration> rootCas;

    public static TestPKI get() {
        return testPKI;
    }

    public static TestPKI load(String testPkiPath) throws Exception {
        testPKI = new TestPKI();

        // load CA config
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = new GetMethod(testPkiPath + "/"
                + ConfigurationServlet.PATH + "?"
                + ConfigurationServlet.ACTION + "=" + ConfigurationServlet.Action.GET);
        httpClient.executeMethod(getMethod);

        InputStream inputStream = getMethod.getResponseBodyAsStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String caString;
        while (null != (caString = br.readLine())) {
            String[] fields = caString.split(ConfigurationServlet.FIELD_SEPERATOR);
            testPKI.addSaveCa(fields[0], fields[1], Long.parseLong(fields[2]), Integer.parseInt(fields[3]));
        }

        // now get private keys and certificates
        for (CAConfiguration rootCa : testPKI.getRoots().values()) {
            loadParentCa(httpClient, testPkiPath, rootCa);
        }

        return testPKI;
    }

    public TestPKI() {

        this.rootCas = new HashMap<String, CAConfiguration>();
    }

    /**
     * Start embedded jetty for this test PKI
     *
     * @throws Exception something went wrong :o
     */
    public void start(String host) throws Exception {

        LOG.debug("host=" + host);
        this.host = host;
        this.server = new Server();
        Connector connector = new LocalConnector();
        this.server.addConnector(connector);
        this.servletPaths = new LinkedList<String>();

        /*
         * Add servlets
         */
        addServlet(ConfigurationServlet.class, "/" + ConfigurationServlet.PATH);
        addServlet(CrlServlet.class, "/" + CrlServlet.PATH);
        addServlet(OcspServlet.class, "/" + OcspServlet.PATH);
        addServlet(CertificateServlet.class, "/" + CertificateServlet.PATH);
        addServlet(PrivateKeyServlet.class, "/" + PrivateKeyServlet.PATH);

        server.start();
        createSocketConnector();

        // save static for access in servlets
        testPKI = this;

        LOG.debug("Test CA started...");
    }

    /**
     * Stop embedded jetty for this test PKI
     *
     * @throws Exception something went wrong :o
     */
    public void stop() throws Exception {

        this.server.stop();
        LOG.debug("Test CA stopped...");
    }

    private void addServlet(Class<? extends HttpServlet> servletClass,
                            String contextPath) {

        Context context = new Context(null, new SessionHandler(),
                new SecurityHandler(), null, null);
        context.setContextPath(contextPath);
        // http://jira.codehaus.org/browse/JETTY-390
        context.setAllowNullPathInfo(true);
        this.server.addHandler(context);

        ServletHandler handler = context.getServletHandler();

        ServletHolder servletHolder = new ServletHolder();
        servletHolder.setClassName(servletClass.getName());
        servletHolder.setName(servletClass.getName());
        handler.addServlet(servletHolder);

        ServletMapping servletMapping = new ServletMapping();
        servletMapping.setServletName(servletClass.getName());
        servletMapping.setPathSpecs(new String[]{"/*", contextPath});
        handler.addServletMapping(servletMapping);

        servletPaths.add(contextPath);
    }

    private void createSocketConnector() throws Exception {

        SocketConnector connector = new SocketConnector();
        connector.setHost(host);
        this.server.addConnector(connector);
        if (this.server.isStarted()) {
            connector.start();
        } else {
            connector.open();
        }

        path = "http://" + host + ":" + connector.getLocalPort();
    }

    public String getPath() {
        return path;
    }

    public List<String> getServletPaths() {
        return this.servletPaths;
    }

    public Map<String, CAConfiguration> getRoots() {
        return this.rootCas;
    }

    public List<CAConfiguration> getLeaves() {

        List<CAConfiguration> leaves = new LinkedList<CAConfiguration>();

        for (CAConfiguration rootCa : this.rootCas.values()) {
            leaves.addAll(getLeaves(rootCa));
        }
        return leaves;
    }

    private List<CAConfiguration> getLeaves(CAConfiguration parent) {

        List<CAConfiguration> leaves = new LinkedList<CAConfiguration>();
        if (parent.getChilds().isEmpty()) {
            leaves.add(parent);
        } else {
            for (CAConfiguration child : parent.getChilds()) {
                leaves.addAll(getLeaves(child));
            }
        }
        return leaves;
    }

    /**
     * Add/Save CA configuration.
     *
     * @param name       CA name
     * @param root       optional root CA
     * @param crlRecords # of CRL records to generate
     * @param crlRefresh CRL refresh in minutes
     * @throws Exception root CA not found, CA already existing, ...
     */
    public void addSaveCa(String name, String root, long crlRecords, int crlRefresh) throws Exception {

//        LOG.debug("Add/Save CA: " + name + " root=" + root
//                + " crlRecords=" + crlRecords + " crlRefresh=" + crlRefresh);


        // find root if needed
        CAConfiguration rootCa = null;
        if (null != root && !root.isEmpty()) {
            rootCa = findCa(root);
            if (null == rootCa) {
                throw new Exception("Root CA " + root + " not found");
            }
        }

        // add/save new config
        CAConfiguration ca = findCa(name);
        if (null == ca) {
            ca = new CAConfiguration(name, crlRecords, crlRefresh);
        } else {
            ca.setCrlRecords(crlRecords);
            ca.setCrlRefresh(crlRefresh);

        }

        // set root/childs
        ca.setRoot(rootCa);
        if (null != rootCa) {
            if (!rootCa.getChilds().contains(ca)) {
                rootCa.getChilds().add(ca);
            }
        } else {
            rootCas.put(name, ca);
        }
    }

    /**
     * Remove specified CA configuration
     *
     * @param caName the Ca's name
     */
    public void removeCa(String caName) {

        LOG.debug("remove CA configuration: " + caName);

        // check roots
        if (null != rootCas.get(caName)) {
            rootCas.remove(caName);
            return;
        }

        // nope, check deeper
        for (CAConfiguration root : rootCas.values()) {
            removeCa(caName, root);
        }
    }

    private void removeCa(String caName, CAConfiguration parent) {

        Iterator<CAConfiguration> iter = parent.getChilds().iterator();
        while (iter.hasNext()) {
            CAConfiguration child = iter.next();
            if (child.getName().equals(caName)) {
                // remove from root
                iter.remove();
                // remove all childs below
                for (CAConfiguration childChild : child.getChilds()) {
                    removeCa(childChild.getName());
                }
            } else {
                removeCa(caName, child);
            }
        }
    }

    /**
     * Find specified CA configuration.
     *
     * @param caName the CA's name
     * @return <code>null</code> if not found
     */
    public CAConfiguration findCa(String caName) {

        // check roots
        CAConfiguration caConfig = rootCas.get(caName);
        if (null != caConfig) {
            return caConfig;
        }

        // nope, check deeper
        for (CAConfiguration root : rootCas.values()) {
            caConfig = findCa(caName, root);
            if (null != caConfig) {
                return caConfig;
            }
        }

        return null;
    }

    private CAConfiguration findCa(String caName, CAConfiguration parent) {

        for (CAConfiguration child : parent.getChilds()) {
            if (child.getName().equals(caName)) {
                return child;
            } else {
                CAConfiguration config = findCa(caName, child);
                if (null != config) {
                    return config;
                }
            }
        }
        return null;
    }

    /**
     * Generate the PKI tree from the configuration
     *
     * @throws Exception something went wrong.
     */
    public void generate() throws Exception {

        LOG.debug("generate");

        for (CAConfiguration caConfig : rootCas.values()) {
            caConfig.generate();
        }

        LOG.debug("generation finished");
    }

    private static void loadParentCa(HttpClient httpClient, String testPkiPath, CAConfiguration parentCa) throws Exception {

        loadCa(httpClient, testPkiPath, parentCa);
        for (CAConfiguration child : parentCa.getChilds()) {
            loadParentCa(httpClient, testPkiPath, child);
        }
    }

    private static void loadCa(HttpClient httpClient, String testPkiPath, CAConfiguration ca) throws Exception {

        LOG.debug("load CA: " + ca.getName());

        CertificateFactory certificateFactory = CertificateFactory
                .getInstance("X.509");

        // load certificate
        URI certificateURI = new URI(testPkiPath + "/"
                + CertificateServlet.PATH + "?"
                + CertificateServlet.CA_QUERY_PARAM + "=" + ca.getName(), false);
        LOG.debug("URI: " + certificateURI.toString());

        GetMethod getMethod = new GetMethod(certificateURI.toString());
        httpClient.executeMethod(getMethod);

        X509Certificate certificate = (X509Certificate) certificateFactory
                .generateCertificate(getMethod.getResponseBodyAsStream());
        ca.setCertificate(certificate);

        // load private key
        URI keyURI = new URI(testPkiPath + "/"
                + PrivateKeyServlet.PATH + "?"
                + PrivateKeyServlet.CA_QUERY_PARAM + "=" + ca.getName(), false);
        getMethod = new GetMethod(keyURI.toString());
        httpClient.executeMethod(getMethod);

        PEMReader pemReader = new PEMReader(new InputStreamReader(getMethod
                .getResponseBodyAsStream()));
        KeyPair keyPair = (KeyPair) pemReader.readObject();
        ca.setKeyPair(keyPair);
    }

}
