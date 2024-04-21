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
 * Action to set AMR goal.
 */
public class AMRGoalAction extends JosmAction {

    /**
     * Constructs a new {@code AMRGoalAction}.
     */
    public AMRGoalAction() {
        super("Set AMR Goal", "mapmode/amrgoal", "Set AMR Goal",
                null, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var ds = getLayerManager().getEditDataSet();
        var way = ds.getLastSelectedWay();
        Collection<Command> cmds = new LinkedList<>();
        cmds.add(new ChangePropertyCommand(way, "line_info", "goal_pose"));
        if (!way.hasKey("goal_id")) {
            var max = ToolsPlugin.getMaxId(ds.getWays(), "goal_id");
            cmds.add(new ChangePropertyCommand(way, "goal_id", Integer.toString(++max)));
        }
        UndoRedoHandler.getInstance().add(new SequenceCommand("Intermediate goal", cmds));
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(OsmUtils.isOsmCollectionEditable(selection) &&
                selection.size() == 1 &&
                selection.stream().anyMatch(o -> o instanceof Way));
    }
}
