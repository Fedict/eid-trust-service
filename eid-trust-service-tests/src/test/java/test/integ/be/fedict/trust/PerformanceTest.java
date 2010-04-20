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
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.junit.Before;
import org.junit.Test;

import test.integ.be.fedict.trust.util.TestUtils;
import be.fedict.trust.client.XKMS2Client;

public class PerformanceTest {

	private static final Log LOG = LogFactory.getLog(PerformanceTest.class);

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	private boolean run = true;

	private static class PerformanceData {
		private final Date date;
		private int count;

		public PerformanceData() {
			this.date = new Date();
			this.count = 0;
		}

		public void inc() {
			this.count++;
		}

		public Date getDate() {
			return this.date;
		}

		public int getCount() {
			return this.count;
		}
	}

	@Test
	public void testValidateEIDCertificate() throws Exception {
		LOG.debug("validate eID authentication certificate.");

		JOptionPane.showMessageDialog(null, "insert your eID card...");

		List<X509Certificate> authnCertificateChain = TestUtils
				.getAuthnCertificateChain();

		JOptionPane.showMessageDialog(null, "OK to remove eID card...");

		XKMS2Client client = new XKMS2Client(
				"http://192.168.1.101/eid-trust-service-ws/xkms2");

		final int INTERVAL_SIZE = 1000 * 5;

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
				this.run = false;
			}
		}

		ResultDialog dialog = new ResultDialog(performance);
		while (dialog.isVisible()) {
			Thread.sleep(1000);
		}
	}

	private class WorkingFrame extends JFrame implements ActionListener {
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

			TimeSeries series = new TimeSeries("Serie 1");

			for (PerformanceData performanceEntry : performance) {
				series.add(new Second(performanceEntry.getDate()),
						performanceEntry.getCount());
			}

			TimeSeriesCollection dataset = new TimeSeriesCollection();
			dataset.addSeries(series);
			this.chart = ChartFactory.createTimeSeriesChart(
					"eID Trust Service Performance History", "Time",
					"Number of XKMS requests", dataset, false, false, false);
			this.chart.setBackgroundPaint(Color.WHITE);
			XYPlot plot = this.chart.getXYPlot();
			plot.setBackgroundPaint(Color.WHITE);
			DateAxis axis = (DateAxis) plot.getDomainAxis();
			axis.setDateFormatOverride(new SimpleDateFormat("H:m:s"));
			ValueAxis valueAxis = plot.getRangeAxis();
			valueAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
			XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
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
					ChartUtilities.saveChartAsPNG(file, this.chart, 800, 600);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null,
							"error saving to file: " + e.getMessage());
				}
			}
		}
	}
}
