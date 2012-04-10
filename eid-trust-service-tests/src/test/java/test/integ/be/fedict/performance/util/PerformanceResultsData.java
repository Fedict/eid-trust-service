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

import java.io.Serializable;
import java.util.List;

public class PerformanceResultsData implements Serializable {

	private static final long serialVersionUID = 1L;

	private int intervalSize;

	private int expectedRevokedCount;
	private List<PerformanceData> performance;
	private List<MemoryData> memory;

	public PerformanceResultsData(int intervalSize,
			List<PerformanceData> performance, int expectedRevoked,
			List<MemoryData> memory) {
		this.intervalSize = intervalSize;
		this.performance = performance;
		this.expectedRevokedCount = expectedRevoked;
		this.memory = memory;
	}

	public int getIntervalSize() {
		return intervalSize;
	}

	public int getExpectedRevokedCount() {
		return expectedRevokedCount;
	}

	public List<PerformanceData> getPerformance() {
		return performance;
	}

	public List<MemoryData> getMemory() {
		return memory;
	}
}
