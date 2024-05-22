/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.event.ActionEvent;
import org.openstreetmap.josm.actions.JosmAction;

public class SftpAction extends JosmAction {

    public SftpAction() {
        super("SFTP", "mapmode/sftpaction", "SFTP", null, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var dialog = new SftpDialog();
        dialog.showDialog();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(true);
    }
}
