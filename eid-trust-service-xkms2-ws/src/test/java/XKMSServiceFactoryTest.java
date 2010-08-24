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

import be.fedict.trust.xkms2.XKMSServiceFactory;
import org.junit.Test;
import org.w3._2002._03.xkms.XKMSService;

import static org.junit.Assert.assertNotNull;

public class XKMSServiceFactoryTest {

    @Test
    public void testGetInstance() throws Exception {
        // operate
        XKMSService service = XKMSServiceFactory.getInstance();

        // verify
        assertNotNull(service);
    }
}
