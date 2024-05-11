/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * MapMode to move selected nodes and ways with arrow keys.
 */
public class KeyMoveMode extends MapMode implements AWTEventListener {

    /**
     * Constructs a new {@code KeyMoveMode}.
     */
    public KeyMoveMode() {
        super("Key Move Mode", "keymovemode", "Key Move Mode",
                null,
                ImageProvider.getCursor("crosshair", null));
    }

    @Override
    public void enterMode() {
        super.enterMode();
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void exitMode() {
        super.exitMode();
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return l instanceof OsmDataLayer;
    }

    @Override
    public void updateEnabledState() {
        setEnabled(getLayerManager().getEditLayer() != null);
    }

    @Override
    public void eventDispatched(AWTEvent e) {
        var shift = (((InputEvent)e).getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            var code = ((KeyEvent)e).getKeyCode();
            EastNorth en =
                    code == KeyEvent.VK_UP    ? new EastNorth( 0.00,  0.05) :
                    code == KeyEvent.VK_DOWN  ? new EastNorth( 0.00, -0.05) :
                    code == KeyEvent.VK_LEFT  ? new EastNorth(-0.05,  0.00) :
                    code == KeyEvent.VK_RIGHT ? new EastNorth( 0.05,  0.00) : null;
            if (en != null) {
                move(shift ? en : en.scale(4));
                MainApplication.getMap().repaint();
            }
        }
    }

    /**
     * Moves selected nodes and ways by the given delta.
     * @param delta the delta
     */
    private void move(EastNorth delta) {
        var ds = getLayerManager().getEditDataSet();
        Collection<Command> cmds = new LinkedList<>();
        var selected = ds.getSelectedNodesAndWays();
        if (selected != null && !selected.isEmpty()) {
            cmds.add(new MoveCommand(selected, delta));
            UndoRedoHandler.getInstance().add(new SequenceCommand("Key move", cmds));
        }
    }
}
