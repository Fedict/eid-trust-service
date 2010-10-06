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

public class TestPKIStartup {

    private static final Log LOG = LogFactory.getLog(TestPKIStartup.class);

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {

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

        // some default PKI setup
        TestPKI.get().addSaveCa("CN=root1", null, 0, 0);
        TestPKI.get().addSaveCa("CN=root2", null, 0, 0);

        TestPKI.get().addSaveCa("CN=Citizen CA", "CN=root1", 718521, 180);

//        TestPKI.get().addSaveCa("CN=CA1", "CN=root1", 50000, 10);
//        TestPKI.get().addSaveCa("CN=CA2", "CN=root1", 30000, 10);
//        TestPKI.get().addSaveCa("CN=CA3", "CN=root1", 25000, 10);
//        TestPKI.get().addSaveCa("CN=CA4", "CN=root2", 20000, 10);
//        TestPKI.get().addSaveCa("CN=CA5", "CN=root2", 15000, 10);
//        TestPKI.get().addSaveCa("CN=CA6", "CN=root2", 10000, 10);

        while (true) {
            Thread.sleep(1000);
        }

    }
}
