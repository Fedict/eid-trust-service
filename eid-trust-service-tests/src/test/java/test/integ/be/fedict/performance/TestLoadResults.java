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
import org.junit.Test;
import test.integ.be.fedict.performance.util.PerformanceResultDialog;
import test.integ.be.fedict.performance.util.PerformanceResultsData;

import javax.swing.*;
import java.io.File;

/**
 * Used for loading in performance test result data generated in non-interactive mode.
 */
public class TestLoadResults {

    private static final Log LOG = LogFactory.getLog(TestLoadResults.class);

    @Test
    public void testLoad() throws Exception {

        File resultsFile = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select results file...");
        int result = fileChooser.showOpenDialog(new JFrame());
        if (JFileChooser.APPROVE_OPTION == result) {
            resultsFile = fileChooser.getSelectedFile();
        }

        if (null != resultsFile) {

            PerformanceResultsData data = PerformanceResultDialog.readResults(resultsFile);

            PerformanceResultDialog dialog = new PerformanceResultDialog(data);
            while (dialog.isVisible()) {
                Thread.sleep(1000);
            }
        }

    }
}
