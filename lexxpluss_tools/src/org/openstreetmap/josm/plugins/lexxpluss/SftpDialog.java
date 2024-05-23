/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import static org.openstreetmap.josm.tools.I18n.tr;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.GBC;

/**
 * Dialog for sftp open/save settings.
 */
public class SftpDialog extends ExtendedDialog {

    /**
     * Columns width.
     */
    private static int columns = 40;

    /**
     * Host name field.
     */
    private JTextField hostName = new JTextField(null, ToolsSettings.getHost(), columns);

    /**
     * User name field.
     */
    private JTextField userName = new JTextField(null, ToolsSettings.getUser(), columns);

    /**
     * Password field.
     */
    private JPasswordField password = new JPasswordField(ToolsSettings.getPassword());

    /**
     * Osm path field.
     */
    private JTextField osmPath = new JTextField(null, ToolsSettings.getOsmPath(), columns);

    /**
     * Constructs a new {@code SftpDialog}.
     */
    public SftpDialog() {
        super(MainApplication.getMainFrame(), "Open/Save using sftp", new String[] {"Open", "Save", "Cancel"}, true);
        contentInsets = new Insets(15, 15, 5, 15);
        var panel = new JPanel(new GridBagLayout());
        addLabelled(panel, "Hostname:", hostName);
        addLabelled(panel, "Username:", userName);
        addLabelled(panel, "Password:", password);
        addLabelled(panel, "Osm path:", osmPath);
        setContent(panel);
        setDefaultButton(3);
    }

    /**
     * Save settings.
     */
    public void saveSettings() {
        System.out.println("Host: " + hostName.getText());
        System.out.println("User: " + userName.getText());
        System.out.println("Password: " + new String(password.getPassword()));
        System.out.println("Osm path: " + osmPath.getText());
        ToolsSettings.setHost(hostName.getText());
        ToolsSettings.setUser(userName.getText());
        ToolsSettings.setPassword(new String(password.getPassword()));
        ToolsSettings.setOsmPath(osmPath.getText());
    }

    /**
     * Add labelled component
     * @param panel panel
     * @param str label
     * @param c component
     */
    private void addLabelled(JPanel panel, String str, Component c) {
        var label = new JLabel(str);
        panel.add(label, GBC.std());
        label.setLabelFor(c);
        panel.add(c, GBC.eol().fill(GBC.HORIZONTAL));
    }
}
