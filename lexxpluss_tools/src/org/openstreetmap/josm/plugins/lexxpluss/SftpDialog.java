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

public class SftpDialog extends ExtendedDialog {

    private static int columns = 40;
    private JTextField hostName = new JTextField(null, null, columns);
    private JTextField userName = new JTextField(null, "lexxpluss", columns);
    private JPasswordField password = new JPasswordField();
    private JTextField osmPath = new JTextField(null, "/home/lexxpluss/osm/map.osm", columns);

    public SftpDialog() {
        super(MainApplication.getMainFrame(), "SFTP", new String[] {"Download", "Upload", "Cancel"}, true);
//        setButtonIcons(new String[] {"ok", "cancel"});
        contentInsets = new Insets(15, 15, 5, 15);
        var panel = new JPanel(new GridBagLayout());
        addLabelled(panel, "Hostname:", hostName);
        addLabelled(panel, "Username:", userName);
        addLabelled(panel, "Password:", password);
        addLabelled(panel, "Osm path:", osmPath);
        setContent(panel);
        setDefaultButton(3);
    }

    private void addLabelled(JPanel panel, String str, Component c) {
        var label = new JLabel(str);
        panel.add(label, GBC.std());
        label.setLabelFor(c);
        panel.add(c, GBC.eol().fill(GBC.HORIZONTAL));
    }
}
