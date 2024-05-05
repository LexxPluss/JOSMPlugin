/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * Plugin class for the LexxPluss Tools plugin.
 */
public class ToolsPlugin extends Plugin {

    /**
     * Constructs a new {@code ToolsPlugin}.
     * @param info plugin information
     */
    public ToolsPlugin(PluginInformation info) {
        super(info);
        var editMenu = MainApplication.getMenu().editMenu;
        int paste_item_index = -1, duplicate_item_index = -1;
        for (int i = 0; i < editMenu.getItemCount(); ++i) {
            var item = editMenu.getItem(i);
            if (item != null) {
                var action = item.getAction();
                if (action == MainApplication.getMenu().paste)
                    paste_item_index = i;
                else if (action == MainApplication.getMenu().duplicate)
                    duplicate_item_index = i;
            }
        }
        if (paste_item_index >= 0) {
            editMenu.remove(paste_item_index);
            MainApplication.getMenu().paste.destroy();
            MainMenu.add(editMenu, new PasteAndRenumberAction(), false, paste_item_index);
        }
        if (duplicate_item_index >= 0) {
            editMenu.remove(duplicate_item_index);
            MainApplication.getMenu().duplicate.destroy();
            MainMenu.add(editMenu, new DuplicateAndRenumberAction(), false, duplicate_item_index);
        }
        var moreMenu = MainApplication.getMenu().moreToolsMenu;
        if (moreMenu.getMenuComponentCount() > 0)
            moreMenu.addSeparator();
        MainMenu.add(moreMenu, new IntermediateGoalAction());
        MainMenu.add(moreMenu, new AMRGoalAction());
        MainMenu.add(moreMenu, new MovableAreaAction());
        MainMenu.add(moreMenu, new SyncAreaAction());
        MainMenu.add(moreMenu, new SafetyAreaAction());
        MainMenu.add(moreMenu, new NonStopAreaAction());
        OsmValidator.addTest(IdDuplicateTest.class);
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame == null && newFrame != null) {
            newFrame.addMapMode(new IconToggleButton(new AGVLineMode()));
            newFrame.addMapMode(new IconToggleButton(new KeyMoveMode()));
            newFrame.addMapMode(new IconToggleButton(new ParkingAreaMode()));
        }
    }

    /**
     * Returns the maximum value of a key in a collection of OSM primitives.
     * @param objs the collection of OSM primitives
     * @param key the key
     * @return the maximum value of the key
     */
    static public int getMaxId(Collection<? extends OsmPrimitive> objs, String key) {
        return objs.stream()
                .map(obj -> obj.get(key))
                .filter(id -> id != null)
                .map(id -> Integer.parseInt(id))
                .reduce(-1, Integer::max);
    }

    /**
     * Renumber tags of nodes and ways.
     * @param objs the collection of nodes and ways
     */
    static public void renumber(Collection<? extends OsmPrimitive> objs) {
        var nodes = objs.stream().filter(o -> o instanceof Node).map(o -> (Node)o).collect(Collectors.toList());
        var ways = objs.stream().filter(o -> o instanceof Way).map(o -> (Way)o).collect(Collectors.toList());
        ways.forEach(w -> {
            w.getNodes().forEach(n -> nodes.remove(n));
            wayIdRenumber(w);
        });
        nodes.forEach(n -> nodeIdRenumber(n));
    }

    /**
     * Renumber node tags.
     * @param node the node
     */
    static public void nodeIdRenumber(Node node) {
        Arrays.asList("agv_node_id", "intermediate_goal_id")
                .forEach(key -> {
                    if (node.hasKey(key)) {
                        var max = ToolsPlugin.getMaxId(node.getDataSet().getNodes(), key);
                        node.put(key, Integer.toString(max + 1));
                    }
                });
    }

    /**
     * Renumber way tags.
     * @param way the way
     */
    static public void wayIdRenumber(Way way) {
        Arrays.asList("goal_id", "space_id", "sync_id")
                .forEach(key -> {
                    if (way.hasKey(key)) {
                        var max = ToolsPlugin.getMaxId(way.getDataSet().getWays(), key);
                        var s = Integer.toString(max + 1);
                        way.put(key, s);
                        if (key.equals("space_id"))
                            way.put("area_name" , "park" + s);
                        else if (key.equals("sync_id"))
                            way.put("area_name" , "sync" + s);
                    }
                });
        way.getNodes().forEach(n -> nodeIdRenumber(n));
    }
}
