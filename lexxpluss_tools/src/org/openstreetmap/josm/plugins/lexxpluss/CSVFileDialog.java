/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * File dialog for CSV files.
 */
public class CSVFileDialog extends JFileChooser {

    /**
     * The current directory.
     */
    private static File currentDirectory = null;

    /**
     * Constructs a new {@code CSVFileDialog}.
     */
    public CSVFileDialog() {
        super();
        if (currentDirectory != null)
            setCurrentDirectory(currentDirectory);
        setFileSelectionMode(JFileChooser.FILES_ONLY);
        setFileFilter(new FileNameExtensionFilter("CSV file", "csv"));
    }

    @Override
    public int showOpenDialog(Component parent) {
        int ret = super.showOpenDialog(parent);
        currentDirectory = getCurrentDirectory();
        return ret;
    }
}
