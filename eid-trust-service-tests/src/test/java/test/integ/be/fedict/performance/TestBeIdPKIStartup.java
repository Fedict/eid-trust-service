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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * An example {@link TestPKI} simulating the Belgian eID PKI.
 * The CA's and # of CRL entries where found using {@link TestHarvestEid}.
 */
public class TestBeIdPKIStartup {

    private static final Log LOG = LogFactory.getLog(TestBeIdPKIStartup.class);

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {

        LOG.debug("sebeco-dev-11");
        new TestPKI().start("sebeco-dev-11");

        for (String servletPath : TestPKI.get().getServletPaths()) {
            LOG.debug("Servlet: " + TestPKI.get().getPath() + servletPath);
        }
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {

        TestPKI.get().stop();
    }

    @Test
    public void testCA() throws Exception {

        // Belgian eid test PKI
        TestPKI.get().addSaveCa("CN=root1", null, 1, 180);
        TestPKI.get().addSaveCa("CN=root2", null, 1, 180);
        TestPKI.get().addSaveCa("CN=Citizen CA", "CN=root1", 718521, 180);
        TestPKI.get().addSaveCa("CN=200501", "CN=root1", 709866, 180);
        TestPKI.get().addSaveCa("CN=200502", "CN=root1", 58135, 180);
        TestPKI.get().addSaveCa("CN=200503", "CN=root1", 191524, 180);
        TestPKI.get().addSaveCa("CN=200504", "CN=root1", 188841, 180);
        TestPKI.get().addSaveCa("CN=200505", "CN=root1", 188346, 180);
        TestPKI.get().addSaveCa("CN=200506", "CN=root1", 192110, 180);
        TestPKI.get().addSaveCa("CN=200507", "CN=root1", 188237, 180);
        TestPKI.get().addSaveCa("CN=200508", "CN=root1", 187693, 180);
        TestPKI.get().addSaveCa("CN=200509", "CN=root1", 189385, 180);
        TestPKI.get().addSaveCa("CN=200510", "CN=root1", 188124, 180);
        TestPKI.get().addSaveCa("CN=200511", "CN=root1", 189760, 180);
        TestPKI.get().addSaveCa("CN=200512", "CN=root1", 187718, 180);
        TestPKI.get().addSaveCa("CN=200513", "CN=root1", 189882, 180);
        TestPKI.get().addSaveCa("CN=200514", "CN=root1", 189442, 180);
        TestPKI.get().addSaveCa("CN=200515", "CN=root1", 188546, 180);
        TestPKI.get().addSaveCa("CN=200601", "CN=root1", 67405, 180);
        TestPKI.get().addSaveCa("CN=200602", "CN=root1", 52986, 180);
        TestPKI.get().addSaveCa("CN=200603", "CN=root1", 52145, 180);
        TestPKI.get().addSaveCa("CN=200604", "CN=root1", 51140, 180);
        TestPKI.get().addSaveCa("CN=200605", "CN=root1", 51798, 180);
        TestPKI.get().addSaveCa("CN=200606", "CN=root1", 51874, 180);
        TestPKI.get().addSaveCa("CN=200607", "CN=root1", 51340, 180);
        TestPKI.get().addSaveCa("CN=200608", "CN=root1", 50924, 180);
        TestPKI.get().addSaveCa("CN=200609", "CN=root1", 52678, 180);
        TestPKI.get().addSaveCa("CN=200610", "CN=root1", 52231, 180);
        TestPKI.get().addSaveCa("CN=200611", "CN=root1", 51070, 180);
        TestPKI.get().addSaveCa("CN=200612", "CN=root1", 50847, 180);
        TestPKI.get().addSaveCa("CN=200613", "CN=root1", 50255, 180);
        TestPKI.get().addSaveCa("CN=200614", "CN=root1", 49690, 180);
        TestPKI.get().addSaveCa("CN=200615", "CN=root1", 52347, 180);
        TestPKI.get().addSaveCa("CN=200616", "CN=root1", 49765, 180);
        TestPKI.get().addSaveCa("CN=200617", "CN=root1", 50630, 180);
        TestPKI.get().addSaveCa("CN=200618", "CN=root1", 51637, 180);
        TestPKI.get().addSaveCa("CN=200619", "CN=root1", 50755, 180);
        TestPKI.get().addSaveCa("CN=200620", "CN=root1", 50899, 180);
        TestPKI.get().addSaveCa("CN=200701", "CN=root1", 51637, 180);
        TestPKI.get().addSaveCa("CN=200702", "CN=root1", 48506, 180);
        TestPKI.get().addSaveCa("CN=200703", "CN=root1", 48563, 180);
        TestPKI.get().addSaveCa("CN=200704", "CN=root1", 49025, 180);
        TestPKI.get().addSaveCa("CN=200705", "CN=root1", 47922, 180);
        TestPKI.get().addSaveCa("CN=200706", "CN=root1", 49189, 180);
        TestPKI.get().addSaveCa("CN=200707", "CN=root1", 48915, 180);
        TestPKI.get().addSaveCa("CN=200708", "CN=root1", 48175, 180);
        TestPKI.get().addSaveCa("CN=200709", "CN=root1", 45739, 180);
        TestPKI.get().addSaveCa("CN=200710", "CN=root1", 47685, 180);
        TestPKI.get().addSaveCa("CN=200711", "CN=root1", 46537, 180);
        TestPKI.get().addSaveCa("CN=200712", "CN=root1", 46474, 180);
        TestPKI.get().addSaveCa("CN=200713", "CN=root1", 40425, 180);
        TestPKI.get().addSaveCa("CN=200714", "CN=root1", 41181, 180);
        TestPKI.get().addSaveCa("CN=200715", "CN=root1", 38748, 180);
        TestPKI.get().addSaveCa("CN=200716", "CN=root1", 39376, 180);
        TestPKI.get().addSaveCa("CN=200801", "CN=root1", 29676, 180);
        TestPKI.get().addSaveCa("CN=200802", "CN=root1", 29940, 180);
        TestPKI.get().addSaveCa("CN=200803", "CN=root1", 29533, 180);
        TestPKI.get().addSaveCa("CN=200804", "CN=root1", 31369, 180);
        TestPKI.get().addSaveCa("CN=200805", "CN=root1", 32027, 180);
        TestPKI.get().addSaveCa("CN=200806", "CN=root1", 41025, 180);
        TestPKI.get().addSaveCa("CN=200807", "CN=root1", 33862, 180);
        TestPKI.get().addSaveCa("CN=200808", "CN=root1", 39381, 180);
        TestPKI.get().addSaveCa("CN=200809", "CN=root1", 11305, 180);
        TestPKI.get().addSaveCa("CN=200810", "CN=root1", 14741, 180);
        TestPKI.get().addSaveCa("CN=200811", "CN=root1", 11497, 180);
        TestPKI.get().addSaveCa("CN=200812", "CN=root1", 11054, 180);
        TestPKI.get().addSaveCa("CN=200813", "CN=root1", 11218, 180);
        TestPKI.get().addSaveCa("CN=200814", "CN=root1", 11146, 180);
        TestPKI.get().addSaveCa("CN=200815", "CN=root1", 11628, 180);
        TestPKI.get().addSaveCa("CN=200816", "CN=root1", 11519, 180);
        TestPKI.get().addSaveCa("CN=200817", "CN=root1", 45464, 180);
        TestPKI.get().addSaveCa("CN=200818", "CN=root1", 42678, 180);
        TestPKI.get().addSaveCa("CN=200819", "CN=root1", 43436, 180);
        TestPKI.get().addSaveCa("CN=200820", "CN=root1", 42806, 180);
        TestPKI.get().addSaveCa("CN=200901", "CN=root2", 55169, 180);
        TestPKI.get().addSaveCa("CN=200902", "CN=root2", 55954, 180);
        TestPKI.get().addSaveCa("CN=200903", "CN=root2", 58922, 180);
        TestPKI.get().addSaveCa("CN=200904", "CN=root2", 46709, 180);
        TestPKI.get().addSaveCa("CN=200905", "CN=root2", 81580, 180);
        TestPKI.get().addSaveCa("CN=200906", "CN=root2", 83811, 180);
        TestPKI.get().addSaveCa("CN=200907", "CN=root2", 77074, 180);
        TestPKI.get().addSaveCa("CN=200908", "CN=root2", 72572, 180);
        TestPKI.get().addSaveCa("CN=200909", "CN=root2", 46431, 180);
        TestPKI.get().addSaveCa("CN=200910", "CN=root2", 47928, 180);
        TestPKI.get().addSaveCa("CN=200911", "CN=root2", 38697, 180);
        TestPKI.get().addSaveCa("CN=200912", "CN=root2", 39617, 180);
        TestPKI.get().addSaveCa("CN=201001", "CN=root2", 103300, 180);
        TestPKI.get().addSaveCa("CN=201002", "CN=root2", 113518, 180);
        TestPKI.get().addSaveCa("CN=201003", "CN=root2", 108167, 180);
        TestPKI.get().addSaveCa("CN=201004", "CN=root2", 99649, 180);
        TestPKI.get().addSaveCa("CN=201005", "CN=root2", 117628, 180);
        TestPKI.get().addSaveCa("CN=201006", "CN=root2", 112949, 180);
        TestPKI.get().addSaveCa("CN=201007", "CN=root2", 108237, 180);
        TestPKI.get().addSaveCa("CN=201008", "CN=root2", 103931, 180);
        TestPKI.get().addSaveCa("CN=201009", "CN=root2", 41216, 180);
        TestPKI.get().addSaveCa("CN=201010", "CN=root2", 43302, 180);
        TestPKI.get().addSaveCa("CN=201011", "CN=root2", 46628, 180);
        TestPKI.get().addSaveCa("CN=201012", "CN=root2", 41230, 180);

        while (true) {
            Thread.sleep(1000);
        }

    }
}
