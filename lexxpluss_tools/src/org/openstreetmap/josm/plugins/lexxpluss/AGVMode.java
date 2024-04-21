/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.util.Collection;
import java.util.LinkedList;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

import java.awt.event.MouseEvent;

/**
 * MapMode for AGV scene creation.
 */
public class AGVMode extends MapMode {

    /**
     * Constructs a new {@code AGVMode}.
     */
    public AGVMode() {
        super("AGV Mode", "agvmode", "AGV Mode",
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
        var ds = getLayerManager().getEditDataSet();
        var nodes = new Node[6];
        nodes[0] = new Node(en_base);
        nodes[1] = new Node(en_base.add( 0.0, 5.0));
        nodes[2] = new Node(en_base.add( 1.0,  0.0));
        nodes[3] = new Node(en_base.add(-1.0,  2.0));
        nodes[4] = new Node(en_base.add(-1.0,  0.0));
        nodes[5] = new Node(en_base.add( 1.0,  2.0));
        var max_node_id = ToolsPlugin.getMaxId(ds.getNodes(), "agv_node_id");
        nodes[0].put("agv_node_id", Integer.toString(max_node_id + 1));
        nodes[1].put("agv_node_id", Integer.toString(max_node_id + 2));
        Collection<Command> cmds = new LinkedList<>();
        var max_goal_id = ToolsPlugin.getMaxId(ds.getWays(), "goal_id");
        addWayCommands(ds, cmds, nodes[0], nodes[1], new TagMap("line_info", "agv_pose"));
        addWayCommands(ds, cmds, nodes[2], nodes[3], new TagMap("line_info", "goal_pose", "goal_id", Integer.toString(max_goal_id + 1)));
        addWayCommands(ds, cmds, nodes[4], nodes[5], new TagMap("line_info", "goal_pose", "goal_id", Integer.toString(max_goal_id + 2)));
        UndoRedoHandler.getInstance().add(new SequenceCommand("Create AGV Scene", cmds));
    }

    /**
     * Adds commands to create a way between two nodes.
     * @param ds the data set
     * @param cmds the collection of commands
     * @param n0 the first node
     * @param n1 the second node
     * @param tags the tags to add to the way
     */
    private void addWayCommands(DataSet ds, Collection<Command> cmds, Node n0, Node n1, TagMap tags) {
        var w = new Way();
        w.addNode(n0);
        w.addNode(n1);
        tags.forEach((k, v) -> w.put(k, v));
        cmds.add(new AddCommand(ds, n0));
        cmds.add(new AddCommand(ds, n1));
        cmds.add(new AddCommand(ds, w));
    }
}
