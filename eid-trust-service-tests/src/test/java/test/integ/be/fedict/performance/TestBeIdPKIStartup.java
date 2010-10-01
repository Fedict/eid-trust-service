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

public class TestBeIdPKIStartup {

    private static final Log LOG = LogFactory.getLog(TestBeIdPKIStartup.class);

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {

        new TestPKI().start("sebeco-dev-10");

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

        // some default PKI setup
//        TestPKI.get().addSaveCa("root1", null, 0, 0);
//
//        TestPKI.get().addSaveCa("CA1", "root1", 50000, 10);
//        TestPKI.get().addSaveCa("CA2", "root1", 30000, 10);
//        TestPKI.get().addSaveCa("CA3", "root1", 25000, 10);
//        TestPKI.get().addSaveCa("CA4", "root1", 20000, 10);
//        TestPKI.get().addSaveCa("CA5", "root1", 15000, 10);
//        TestPKI.get().addSaveCa("CA6", "root1", 10000, 10);

        // Belgian eid test PKI
        TestPKI.get().addSaveCa("CN=root1", null, 0, 0);
        TestPKI.get().addSaveCa("CN=root2", null, 0, 0);
        TestPKI.get().addSaveCa("CN=Citizen CA", "CN=root1", 718521, 0);
        TestPKI.get().addSaveCa("CN=200501", "CN=root1", 709866, 0);
        TestPKI.get().addSaveCa("CN=200502", "CN=root1", 58135, 0);
        TestPKI.get().addSaveCa("CN=200503", "CN=root1", 191524, 0);
        TestPKI.get().addSaveCa("CN=200504", "CN=root1", 188841, 0);
        TestPKI.get().addSaveCa("CN=200505", "CN=root1", 188346, 0);
        TestPKI.get().addSaveCa("CN=200506", "CN=root1", 192110, 0);
        TestPKI.get().addSaveCa("CN=200507", "CN=root1", 188237, 0);
        TestPKI.get().addSaveCa("CN=200508", "CN=root1", 187693, 0);
        TestPKI.get().addSaveCa("CN=200509", "CN=root1", 189385, 0);
        TestPKI.get().addSaveCa("CN=200510", "CN=root1", 188124, 0);
        TestPKI.get().addSaveCa("CN=200511", "CN=root1", 189760, 0);
        TestPKI.get().addSaveCa("CN=200512", "CN=root1", 187718, 0);
        TestPKI.get().addSaveCa("CN=200513", "CN=root1", 189882, 0);
        TestPKI.get().addSaveCa("CN=200514", "CN=root1", 189442, 0);
        TestPKI.get().addSaveCa("CN=200515", "CN=root1", 188546, 0);
        TestPKI.get().addSaveCa("CN=200601", "CN=root1", 67405, 0);
        TestPKI.get().addSaveCa("CN=200602", "CN=root1", 52986, 0);
        TestPKI.get().addSaveCa("CN=200603", "CN=root1", 52145, 0);
        TestPKI.get().addSaveCa("CN=200604", "CN=root1", 51140, 0);
        TestPKI.get().addSaveCa("CN=200605", "CN=root1", 51798, 0);
        TestPKI.get().addSaveCa("CN=200606", "CN=root1", 51874, 0);
        TestPKI.get().addSaveCa("CN=200607", "CN=root1", 51340, 0);
        TestPKI.get().addSaveCa("CN=200608", "CN=root1", 50924, 0);
        TestPKI.get().addSaveCa("CN=200609", "CN=root1", 52678, 0);
        TestPKI.get().addSaveCa("CN=200610", "CN=root1", 52231, 0);
        TestPKI.get().addSaveCa("CN=200611", "CN=root1", 51070, 0);
        TestPKI.get().addSaveCa("CN=200612", "CN=root1", 50847, 0);
        TestPKI.get().addSaveCa("CN=200613", "CN=root1", 50255, 0);
        TestPKI.get().addSaveCa("CN=200614", "CN=root1", 49690, 0);
        TestPKI.get().addSaveCa("CN=200615", "CN=root1", 52347, 0);
        TestPKI.get().addSaveCa("CN=200616", "CN=root1", 49765, 0);
        TestPKI.get().addSaveCa("CN=200617", "CN=root1", 50630, 0);
        TestPKI.get().addSaveCa("CN=200618", "CN=root1", 51637, 0);
        TestPKI.get().addSaveCa("CN=200619", "CN=root1", 50755, 0);
        TestPKI.get().addSaveCa("CN=200620", "CN=root1", 50899, 0);
        TestPKI.get().addSaveCa("CN=200701", "CN=root1", 51637, 0);
        TestPKI.get().addSaveCa("CN=200702", "CN=root1", 48506, 0);
        TestPKI.get().addSaveCa("CN=200703", "CN=root1", 48563, 0);
        TestPKI.get().addSaveCa("CN=200704", "CN=root1", 49025, 0);
        TestPKI.get().addSaveCa("CN=200705", "CN=root1", 47922, 0);
        TestPKI.get().addSaveCa("CN=200706", "CN=root1", 49189, 0);
        TestPKI.get().addSaveCa("CN=200707", "CN=root1", 48915, 0);
        TestPKI.get().addSaveCa("CN=200708", "CN=root1", 48175, 0);
        TestPKI.get().addSaveCa("CN=200709", "CN=root1", 45739, 0);
        TestPKI.get().addSaveCa("CN=200710", "CN=root1", 47685, 0);
        TestPKI.get().addSaveCa("CN=200711", "CN=root1", 46537, 0);
        TestPKI.get().addSaveCa("CN=200712", "CN=root1", 46474, 0);
        TestPKI.get().addSaveCa("CN=200713", "CN=root1", 40425, 0);
        TestPKI.get().addSaveCa("CN=200714", "CN=root1", 41181, 0);
        TestPKI.get().addSaveCa("CN=200715", "CN=root1", 38748, 0);
        TestPKI.get().addSaveCa("CN=200716", "CN=root1", 39376, 0);
        TestPKI.get().addSaveCa("CN=200801", "CN=root1", 29676, 0);
        TestPKI.get().addSaveCa("CN=200802", "CN=root1", 29940, 0);
        TestPKI.get().addSaveCa("CN=200803", "CN=root1", 29533, 0);
        TestPKI.get().addSaveCa("CN=200804", "CN=root1", 31369, 0);
        TestPKI.get().addSaveCa("CN=200805", "CN=root1", 32027, 0);
        TestPKI.get().addSaveCa("CN=200806", "CN=root1", 41025, 0);
        TestPKI.get().addSaveCa("CN=200807", "CN=root1", 33862, 0);
        TestPKI.get().addSaveCa("CN=200808", "CN=root1", 39381, 0);
        TestPKI.get().addSaveCa("CN=200809", "CN=root1", 11305, 0);
        TestPKI.get().addSaveCa("CN=200810", "CN=root1", 14741, 0);
        TestPKI.get().addSaveCa("CN=200811", "CN=root1", 11497, 0);
        TestPKI.get().addSaveCa("CN=200812", "CN=root1", 11054, 0);
        TestPKI.get().addSaveCa("CN=200813", "CN=root1", 11218, 0);
        TestPKI.get().addSaveCa("CN=200814", "CN=root1", 11146, 0);
        TestPKI.get().addSaveCa("CN=200815", "CN=root1", 11628, 0);
        TestPKI.get().addSaveCa("CN=200816", "CN=root1", 11519, 0);
        TestPKI.get().addSaveCa("CN=200817", "CN=root1", 45464, 0);
        TestPKI.get().addSaveCa("CN=200818", "CN=root1", 42678, 0);
        TestPKI.get().addSaveCa("CN=200819", "CN=root1", 43436, 0);
        TestPKI.get().addSaveCa("CN=200820", "CN=root1", 42806, 0);
        TestPKI.get().addSaveCa("CN=200901", "CN=root2", 55169, 0);
        TestPKI.get().addSaveCa("CN=200902", "CN=root2", 55954, 0);
        TestPKI.get().addSaveCa("CN=200903", "CN=root2", 58922, 0);
        TestPKI.get().addSaveCa("CN=200904", "CN=root2", 46709, 0);
        TestPKI.get().addSaveCa("CN=200905", "CN=root2", 81580, 0);
        TestPKI.get().addSaveCa("CN=200906", "CN=root2", 83811, 0);
        TestPKI.get().addSaveCa("CN=200907", "CN=root2", 77074, 0);
        TestPKI.get().addSaveCa("CN=200908", "CN=root2", 72572, 0);
        TestPKI.get().addSaveCa("CN=200909", "CN=root2", 46431, 0);
        TestPKI.get().addSaveCa("CN=200910", "CN=root2", 47928, 0);
        TestPKI.get().addSaveCa("CN=200911", "CN=root2", 38697, 0);
        TestPKI.get().addSaveCa("CN=200912", "CN=root2", 39617, 0);
        TestPKI.get().addSaveCa("CN=201001", "CN=root2", 103300, 0);
        TestPKI.get().addSaveCa("CN=201002", "CN=root2", 113518, 0);
        TestPKI.get().addSaveCa("CN=201003", "CN=root2", 108167, 0);
        TestPKI.get().addSaveCa("CN=201004", "CN=root2", 99649, 0);
        TestPKI.get().addSaveCa("CN=201005", "CN=root2", 117628, 0);
        TestPKI.get().addSaveCa("CN=201006", "CN=root2", 112949, 0);
        TestPKI.get().addSaveCa("CN=201007", "CN=root2", 108237, 0);
        TestPKI.get().addSaveCa("CN=201008", "CN=root2", 103931, 0);
        TestPKI.get().addSaveCa("CN=201009", "CN=root2", 41216, 0);
        TestPKI.get().addSaveCa("CN=201010", "CN=root2", 43302, 0);
        TestPKI.get().addSaveCa("CN=201011", "CN=root2", 46628, 0);
        TestPKI.get().addSaveCa("CN=201012", "CN=root2", 41230, 0);

        while (true) {
            Thread.sleep(1000);
        }

    }
}
