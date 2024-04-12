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
import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Action to set intermediate goal.
 */
public class IntermediateGoalAction extends JosmAction {

    /**
     * Constructs a new {@code IntermediateGoalAction}.
     */
    public IntermediateGoalAction() {
        super("Set Intermediate Goal", "mapmode/intermediategoal", "Set Intermediate Goal",
                null, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var ds = getLayerManager().getEditDataSet();
        var way = ds.getLastSelectedWay();
        var count = way.getNodesCount();
        var nodes = way.getNodes();
        var n0 = nodes.get(0);
        var n1 = nodes.get(count - 1);
        ds.setSelected(n0);
        SplitWayAction.runOn(ds);
        ds.setSelected(n1);
        SplitWayAction.runOn(ds);
        ds.setSelected(way);
        Collection<Command> cmds = new LinkedList<>();
        cmds.add(new ChangePropertyCommand(way, "line_info", "\"\""));
        cmds.add(new ChangePropertyCommand(way, "oneway", "yes"));
        var max = ToolsPlugin.getMaxId(ds.getNodes(), "intermediate_goal_id");
        var key = "intermediate_goal_id";
        if (!n0.hasKey(key))
            cmds.add(new ChangePropertyCommand(n0, key, Integer.toString(max + 1)));
        if (!n1.hasKey(key))
            cmds.add(new ChangePropertyCommand(n1, key, Integer.toString(max + 2)));
        UndoRedoHandler.getInstance().add(new SequenceCommand("Intermediate goal", cmds));
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(OsmUtils.isOsmCollectionEditable(selection) &&
                selection.size() == 1 &&
                selection.stream().anyMatch(o -> o instanceof Way));
    }
}
