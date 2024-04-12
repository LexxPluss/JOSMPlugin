/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.util.Collection;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
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
        var menu = MainApplication.getMenu().moreToolsMenu;
        if (menu.getMenuComponentCount() > 0)
            menu.addSeparator();
        MainMenu.add(menu, new IntermediateGoalAction());
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame == null && newFrame != null)
            newFrame.addMapMode(new IconToggleButton(new AGVMode()));
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
}
