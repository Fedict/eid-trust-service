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

package test.integ.be.fedict.trust;

import java.awt.Color;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import org.junit.Before;
import org.junit.Test;

import test.integ.be.fedict.trust.util.TestUtils;
import be.fedict.trust.client.XKMS2Client;

public class PerformanceTest {

	private static final Log LOG = LogFactory.getLog(PerformanceTest.class);

	// private static final String XKMS_LOCATION =
	// "http://www.e-contract.be/eid-trust-service-ws/xkms2";

	private static final String XKMS_LOCATION =
	 "http://192.168.1.101/eid-trust-service-ws/xkms2";
	//private static final String XKMS_LOCATION = "http://sebeco-dev-11:8080/eid-trust-service-ws/xkms2";

	private static final int INTERVAL_SIZE = 1000 * 10;

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	private boolean run = true;

	private static class PerformanceData {
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

	@Test
	public void testValidateEIDCertificate() throws Exception {
		LOG.debug("validate eID authentication certificate.");

		JOptionPane.showMessageDialog(null, "insert your eID card...");

		List<X509Certificate> authnCertificateChain = TestUtils
				.getAuthnCertificateChain();

		JOptionPane.showMessageDialog(null, "OK to remove eID card...");

		XKMS2Client client = new XKMS2Client(XKMS_LOCATION);

		List<PerformanceData> performance = new LinkedList<PerformanceData>();
		PerformanceData currentPerformance = new PerformanceData();
		performance.add(currentPerformance);
		long nextIntervalT = System.currentTimeMillis() + INTERVAL_SIZE;

		new WorkingFrame();

		while (this.run) {
			try {
				client.validate(authnCertificateChain);
				currentPerformance.inc();
				if (System.currentTimeMillis() > nextIntervalT) {
					currentPerformance = new PerformanceData();
					nextIntervalT = System.currentTimeMillis() + INTERVAL_SIZE;
					performance.add(currentPerformance);
				}
			} catch (Exception e) {
				LOG.error("error: " + e.getMessage(), e);
				currentPerformance.incFailures();
			}
		}

		ResultDialog dialog = new ResultDialog(performance);
		while (dialog.isVisible()) {
			Thread.sleep(1000);
		}
	}

	private class WorkingFrame extends JFrame implements ActionListener {
		private static final long serialVersionUID = 1L;

		public WorkingFrame() {
			super("Running performance tests");
			setSize(400, 100);

			Container container = getContentPane();
			JButton quitButton = new JButton("End");
			container.add(quitButton);
			quitButton.addActionListener(this);

			setVisible(true);
		}

		public void actionPerformed(ActionEvent e) {
			PerformanceTest.this.run = false;
			setVisible(false);
			dispose();
		}
	}

	private class ResultDialog extends JDialog implements ActionListener {

		private static final long serialVersionUID = 1L;
		private JFreeChart chart;

		public ResultDialog(List<PerformanceData> performance) {
			super((Frame) null, "Performance test results");
			setSize(400, 300);

			JMenuBar menuBar = new JMenuBar();
			setJMenuBar(menuBar);
			JMenu fileMenu = new JMenu("File");
			menuBar.add(fileMenu);
			JMenuItem saveMenuItem = new JMenuItem("Save");
			fileMenu.add(saveMenuItem);
			saveMenuItem.addActionListener(this);

			TimeSeries series = new TimeSeries("Success");
			TimeSeries failureSeries = new TimeSeries("Failures");

			performance.remove(performance.size() - 1);

			int totalCount = 0;
			int totalFailures = 0;

			for (PerformanceData performanceEntry : performance) {
				series.add(new Second(performanceEntry.getDate()),
						performanceEntry.getCount());
				totalCount += performanceEntry.getCount();

				failureSeries.add(new Second(performanceEntry.getDate()),
						performanceEntry.getFailures());
				totalFailures += performanceEntry.getFailures();
			}

			TimeSeriesCollection dataset = new TimeSeriesCollection();
			dataset.addSeries(series);
			dataset.addSeries(failureSeries);
			this.chart = ChartFactory.createTimeSeriesChart(
					"eID Trust Service Performance History",
					"Time (interval size " + INTERVAL_SIZE + " msec)",
					"Number of XKMS requests", dataset, true, false, false);
			this.chart.addSubtitle(new TextTitle(performance.get(0).getDate()
					.toString()));

			TextTitle info = new TextTitle("Total number of requests: "
					+ totalCount);
			info.setTextAlignment(HorizontalAlignment.LEFT);
			info.setPosition(RectangleEdge.BOTTOM);
			this.chart.addSubtitle(info);
			TextTitle info2 = new TextTitle("Total number of failures: "
					+ totalFailures);
			info2.setPosition(RectangleEdge.BOTTOM);
			info2.setTextAlignment(HorizontalAlignment.LEFT);
			this.chart.addSubtitle(info2);
			this.chart.setBackgroundPaint(Color.WHITE);
			XYPlot plot = this.chart.getXYPlot();
			plot.setBackgroundPaint(Color.WHITE);
			DateAxis axis = (DateAxis) plot.getDomainAxis();
			axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
			ValueAxis valueAxis = plot.getRangeAxis();
			valueAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
			XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
			plot.setRangeGridlinePaint(Color.black);
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
}
