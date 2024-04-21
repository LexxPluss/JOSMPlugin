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
        var editMenu = MainApplication.getMenu().editMenu;
        int paste_item_index = -1;
        for (int i = 0; i < editMenu.getItemCount(); ++i) {
            var item = editMenu.getItem(i);
            if (item != null && item.getAction() == MainApplication.getMenu().paste) {
                paste_item_index = i;
                break;
            }
        }
        if (paste_item_index >= 0) {
            editMenu.remove(paste_item_index);
            MainApplication.getMenu().paste.destroy();
            MainMenu.add(editMenu, new PasteAndRenumberAction(), false, paste_item_index);
        }
        var moreMenu = MainApplication.getMenu().moreToolsMenu;
        if (moreMenu.getMenuComponentCount() > 0)
            moreMenu.addSeparator();
        MainMenu.add(moreMenu, new AMRGoalAction());
        MainMenu.add(moreMenu, new IntermediateGoalAction());
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame == null && newFrame != null) {
            newFrame.addMapMode(new IconToggleButton(new AGVLineMode()));
            newFrame.addMapMode(new IconToggleButton(new KeyMoveMode()));
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
}
