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

package test.integ.be.fedict.performance.util;

import java.util.Date;

public class PerformanceData {

    private final Date date;
    private int count;
    private int failures;

    public PerformanceData() {
        this.date = new Date();
        this.count = 0;
        this.failures = 0;
    }

    public void inc() {
        this.count++;
    }

    public void incFailures() {
        this.failures++;
    }

    public Date getDate() {
        return this.date;
    }

    public int getCount() {
        return this.count;
    }

    public int getFailures() {
        return this.failures;
    }
}
