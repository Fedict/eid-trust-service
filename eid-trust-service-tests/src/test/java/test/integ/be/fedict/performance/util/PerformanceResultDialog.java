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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class PerformanceResultDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private JFreeChart chart;

    public PerformanceResultDialog(int intervalSize,
                                   List<PerformanceData> performance,
                                   int expectedRevoked) {

        super((Frame) null, "Performance test results");
        setSize(500, 400);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem saveMenuItem = new JMenuItem("Save");
        fileMenu.add(saveMenuItem);
        saveMenuItem.addActionListener(this);

        TimeSeries series = new TimeSeries("Success");
        TimeSeries revokedSeries = new TimeSeries("Revoked");
        TimeSeries failureSeries = new TimeSeries("Failures");

        performance.remove(performance.size() - 1);
        if (performance.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "test did not run long enough");
            return;
        }

        int totalCount = 0;
        int totalRevoked = 0;
        int totalFailures = 0;

        for (PerformanceData performanceEntry : performance) {
            series.add(new Second(performanceEntry.getDate()),
                    performanceEntry.getCount());
            totalCount += performanceEntry.getCount();

            revokedSeries.add(new Second(performanceEntry.getDate()),
                    performanceEntry.getRevoked());
            totalRevoked += performanceEntry.getRevoked();

            failureSeries.add(new Second(performanceEntry.getDate()),
                    performanceEntry.getFailures());
            totalFailures += performanceEntry.getFailures();
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);
        dataset.addSeries(revokedSeries);
        dataset.addSeries(failureSeries);
        this.chart = ChartFactory.createTimeSeriesChart(
                "eID Trust Service Performance History",
                "Time (interval size " + intervalSize + " msec)",
                "Number of XKMS requests", dataset, true, false, false);

        this.chart.addSubtitle(new TextTitle(performance.get(0).getDate()
                .toString()
                + " - "
                + performance.get(performance.size() - 1).getDate()
                .toString()));

        TextTitle info = new TextTitle(
                "Total number of successful requests: " + totalCount);
        info.setTextAlignment(HorizontalAlignment.LEFT);
        info.setPosition(RectangleEdge.BOTTOM);
        this.chart.addSubtitle(info);

        TextTitle info2 = new TextTitle("Total number of revoked: "
                + totalRevoked + " expected=" + expectedRevoked);
        info2.setPosition(RectangleEdge.BOTTOM);
        info2.setTextAlignment(HorizontalAlignment.LEFT);
        this.chart.addSubtitle(info2);

        TextTitle info3 = new TextTitle("Total number of failures: "
                + totalFailures);
        info3.setPosition(RectangleEdge.BOTTOM);
        info3.setTextAlignment(HorizontalAlignment.LEFT);
        this.chart.addSubtitle(info3);

        this.chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = this.chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        ValueAxis valueAxis = plot.getRangeAxis();
        valueAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRangeGridlinePaint(Color.black);
        plot.setDomainGridlinePaint(Color.black);
        plot.setRenderer(renderer);

        ChartPanel chartPanel = new ChartPanel(this.chart);
        Container container = getContentPane();
        container.add(chartPanel);

        setVisible(true);
    }

    public void actionPerformed(ActionEvent event) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save as PNG...");
        int result = fileChooser.showSaveDialog(this);
        if (JFileChooser.APPROVE_OPTION == result) {
            File file = fileChooser.getSelectedFile();
            try {
                ChartUtilities.saveChartAsPNG(file, this.chart, 1024, 768);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "error saving to file: " + e.getMessage());
            }
        }
    }
}
