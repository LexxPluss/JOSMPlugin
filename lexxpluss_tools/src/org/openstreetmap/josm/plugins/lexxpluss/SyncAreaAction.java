/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedList;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Action to set sync area.
 */
public class SyncAreaAction extends JosmAction {

    /**
     * Constructs a new {@code SyncAreaAction}.
     */
    public SyncAreaAction() {
        super("Set Sync Area", "mapmode/syncareaaction", "Set Sync Area",
                null, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var ds = getLayerManager().getEditDataSet();
        var way = ds.getLastSelectedWay();
        var max = ToolsPlugin.getMaxId(ds.getWays(), "sync_id");
        var s = Integer.toString(max + 1);
        Collection<Command> cmds = new LinkedList<>();
        cmds.add(new ChangePropertyCommand(way, "sync_id", s));
        cmds.add(new ChangePropertyCommand(way, "area_base", "movable"));
        cmds.add(new ChangePropertyCommand(way, "area_name", "sync" + s));
        cmds.add(new ChangePropertyCommand(way, "area_info", "sync_area"));
        UndoRedoHandler.getInstance().add(new SequenceCommand("Sync area", cmds));
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(OsmUtils.isOsmCollectionEditable(selection) &&
                selection.size() == 1 &&
                selection.stream().anyMatch(o -> o instanceof Way && ((Way)o).isArea()));
    }
}
