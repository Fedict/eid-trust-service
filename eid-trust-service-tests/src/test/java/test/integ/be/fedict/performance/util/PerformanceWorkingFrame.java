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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PerformanceWorkingFrame extends JFrame implements ActionListener,
		Runnable {
	private static final long serialVersionUID = 1L;

	private final PerformanceTest performanceTest;

	private final JLabel countLabel;
	private final JLabel revokedLabel;
	private final JLabel performanceCountLabel;

	private final Thread updateThread;

	public PerformanceWorkingFrame(PerformanceTest performanceTest) {
		super("Running performance tests");

		this.performanceTest = performanceTest;

		setSize(400, 150);

		Container container = getContentPane();

		JPanel infoPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.ipadx = 15;
		infoPanel.add(new JLabel("Request count:"), c);

		c.gridx++;
		this.countLabel = new JLabel("0");
		infoPanel.add(this.countLabel, c);

		c.gridx = 0;
		c.gridy++;
		infoPanel.add(new JLabel("Revoked count:"), c);

		c.gridx++;
		this.revokedLabel = new JLabel("0");
		infoPanel.add(this.revokedLabel, c);

		c.gridx = 0;
		c.gridy++;
		infoPanel.add(new JLabel("Interval count:"), c);

		c.gridx++;
		this.performanceCountLabel = new JLabel("0");
		infoPanel.add(this.performanceCountLabel, c);

		container.add(infoPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton quitButton = new JButton("End");
		buttonPanel.add(quitButton);
		container.add(buttonPanel, BorderLayout.SOUTH);
		quitButton.addActionListener(this);

		setVisible(true);

		this.updateThread = new Thread(this);
		this.updateThread.start();
	}

	public void actionPerformed(ActionEvent event) {

		this.performanceTest.stop();

		try {
			this.updateThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("thread joining error: "
					+ e.getMessage(), e);
		}
		setVisible(false);
		dispose();
	}

	public void run() {
		while (this.performanceTest.isRunning()) {
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					PerformanceWorkingFrame.this.countLabel.setText(Integer
							.toString(performanceTest.getCount()));
					PerformanceWorkingFrame.this.revokedLabel.setText(Integer
							.toString(performanceTest.getRevokedCount()));
					PerformanceWorkingFrame.this.performanceCountLabel
							.setText(Integer.toString(performanceTest
									.getIntervalCount()));
				}
			});
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException("sleep error: " + e.getMessage(), e);
			}
		}
	}
}
