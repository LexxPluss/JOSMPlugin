/**
 * Copyright (c) 2025, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.Notification;


/**
 * Action to set X/Y_image tag coordinate listener plugin
 */
public class CoordChangeListenerAction extends JosmAction {

    private CoordChangeListenerPlugin coordListener = new CoordChangeListenerPlugin();
    private boolean listenerActive = false;

    /**
     * Constructs a new {@code CoordChangeListenerAction}.
     */
    public CoordChangeListenerAction() {
        super("Set Coordinate Listener", "mapmode/coord-change-listener", "Set Listener for Node X/Y_image coord change", null, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        toggleCoordListener();
    }

    private void toggleCoordListener() {
        OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();
        if (layer != null) {
            if (this.listenerActive) {
                layer.data.removeDataSetListener(this.coordListener);
                this.listenerActive = false;
                showNotification("Coordinate listener disabled");
            } else {
                layer.data.addDataSetListener(this.coordListener);
                this.listenerActive = true;
                showNotification("Coordinate listener enabled");
            }
        }
    }

    private void showNotification(String message) {
        new Notification(message)
            .setIcon(JOptionPane.INFORMATION_MESSAGE)
            .setDuration(3000)
            .show();
    }
}
