/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * MapMode for parking area creation.
 */
public class ParkingAreaMode extends MapMode {

    /**
     * Constructs a new {@code ParkingAreaMode}.
     */
    public ParkingAreaMode() {
        super("Parking Area Mode", "parkingareamode", "Parking Area Mode",
                null,
                ImageProvider.getCursor("crosshair", null));
    }

    @Override
    public void enterMode() {
        super.enterMode();
        MainApplication.getMap().mapView.addMouseListener(this);
    }

    @Override
    public void exitMode() {
        super.exitMode();
        MainApplication.getMap().mapView.removeMouseListener(this);
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
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        var pos = e.getPoint();
        var en_base = MainApplication.getMap().mapView.getEastNorth(pos.x, pos.y);
        var nodes = new Node[4];
        nodes[0] = new Node(en_base.add(-1.0, -1.0));
        nodes[1] = new Node(en_base.add(-1.0,  1.0));
        nodes[2] = new Node(en_base.add( 1.0,  1.0));
        nodes[3] = new Node(en_base.add( 1.0, -1.0));
        var ds = getLayerManager().getEditDataSet();
        var max = ToolsPlugin.getMaxId(ds.getWays(), "space_id");
        var s = Integer.toString(max + 1);
        var w = new Way();
        w.put("area_base", "movable");
        w.put("area_detect", "1");
        w.put("area_name", "park" + s);
        w.put("space_id", s);
        Collection<Command> cmds = new LinkedList<>();
        Arrays.stream(nodes).forEach(n -> {
            cmds.add(new AddCommand(ds, n));
            w.addNode(n);
        });
        w.addNode(nodes[0]);
        cmds.add(new AddCommand(ds, w));
        UndoRedoHandler.getInstance().add(new SequenceCommand("Parking Area", cmds));
    }
}
