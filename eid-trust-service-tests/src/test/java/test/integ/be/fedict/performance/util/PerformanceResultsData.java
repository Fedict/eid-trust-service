package test.integ.be.fedict.performance.util;

import java.io.Serializable;
import java.util.List;

public class PerformanceResultsData implements Serializable {

    private int intervalSize;

    private int expectedRevokedCount;
    private List<PerformanceData> performance;
    private List<MemoryData> memory;

    public PerformanceResultsData(int intervalSize,
                                  List<PerformanceData> performance,
                                  int expectedRevoked,
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
