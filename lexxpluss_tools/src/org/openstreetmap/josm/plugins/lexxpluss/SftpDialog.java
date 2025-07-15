/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
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
     * Use identity checkbox.
     */
    private JCheckBox useIdentity = new JCheckBox("Use identity", ToolsSettings.getUseIdentity());

    /**
     * Use auto node setting checkbox.
     */
    private JCheckBox useAutoNodeCoords = new JCheckBox("Set the node coordinates automatically", ToolsSettings.getUseAutoNodeCoords());

    /**
     * Identity path field.
     */
    private JTextField identityPath = new JTextField(null, ToolsSettings.getIdentityPath(), columns);

    /**
     * Osm path field.
     */
    private JTextField osmPath = new JTextField(null, ToolsSettings.getOsmPath(), columns);

    /**
     * Constructs a new {@code SftpDialog}.
     */
    public SftpDialog() {
        super(MainApplication.getMainFrame(), "Open/Save using sftp for LexxPluss", new String[] {"Open", "Save", "Cancel"}, true);
        contentInsets = new Insets(15, 15, 5, 15);
        var panel = new JPanel(new GridBagLayout());
        addLabelled(panel, "Hostname:", hostName);
        addLabelled(panel, "Username:", userName);
        addLabelled(panel, "Password:", password);
        addIdentityUI(panel);
        addLabelled(panel, "Osm path:", osmPath);
        setContent(panel);
        setDefaultButton(3);
    }

    /**
     * Show dialog.
     * @param savable if true then save button is enabled
     */
    public void showDialog(boolean savable) {
        setupDialog();
        if (buttons.size() > 1)
            buttons.get(1).setEnabled(savable);
        showDialog();
    }

    /**
     * Save settings.
     */
    public void saveSettings() {
        ToolsSettings.setHost(hostName.getText());
        ToolsSettings.setUser(userName.getText());
        ToolsSettings.setPassword(new String(password.getPassword()));
        ToolsSettings.setUseIdentity(useIdentity.isSelected());
        ToolsSettings.setUseAutoNodeCoords(useAutoNodeCoords.isSelected());
        ToolsSettings.setIdentityPath(identityPath.getText());
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

    /**
     * Add identity UI.
     * @param panel panel
     */
    private void addIdentityUI(JPanel panel) {
        panel.add(useIdentity, GBC.std());
        panel.add(identityPath, GBC.std());
        var button = new JButton("Select...");
        panel.add(button, GBC.eol().fill(GBC.HORIZONTAL));
        useIdentity.addItemListener(e -> {
            var selected = e.getStateChange() == ItemEvent.SELECTED;
            identityPath.setEnabled(selected);
            button.setEnabled(selected);
        });
        var selected = useIdentity.isSelected();
        identityPath.setEnabled(selected);
        button.setEnabled(selected);
        button.addActionListener(e -> {
            var chooser = new JFileChooser();
            chooser.setFileHidingEnabled(false);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            var ret = chooser.showOpenDialog(this);
            if (ret == JFileChooser.APPROVE_OPTION)
                identityPath.setText(chooser.getSelectedFile().getAbsolutePath());
        });
    }
}
