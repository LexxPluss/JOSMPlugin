/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.AbstractPasteAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action to paste and renumber.
 */
public class PasteAndRenumberAction extends AbstractPasteAction {

    /**
     * Constructs a new {@code PasteAndRenumberAction}.
     */
    public PasteAndRenumberAction() {
        super(tr("Paste (+Renumber)"), "paste", tr("Paste LexxPluss contents of clipboard."),
                Shortcut.registerShortcut("system:paste", tr("Edit: {0}", tr("Paste (+Renumber)")), KeyEvent.VK_V, Shortcut.CTRL), true);
        setHelpId(ht("/Action/Paste"));
        MainApplication.registerActionShortcut(this,
                Shortcut.registerShortcut("system:paste:cua", tr("Edit: {0}", tr("Paste (+Renumber)")), KeyEvent.VK_INSERT, Shortcut.SHIFT));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        var ds = getLayerManager().getEditDataSet();
        var objs = ds.getSelectedNodesAndWays();
        if (objs != null) {
            objs.forEach(o -> {
                if (o instanceof Node)
                    nodeIdRenumber((Node)o);
                else if (o instanceof Way)
                    wayIdRenumber((Way)o);
            });
        }
    }

    /**
     * Renumber node tags.
     * @param node the node
     */
    private void nodeIdRenumber(Node node) {
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
    private void wayIdRenumber(Way way) {
        Arrays.asList("goal_id", "space_id")
                .forEach(key -> {
                    if (way.hasKey(key)) {
                        var max = ToolsPlugin.getMaxId(way.getDataSet().getWays(), key);
                        var s = Integer.toString(max + 1);
                        way.put(key, s);
                        if (key.equals("space_id")) {
                            way.put("area_name" , "park" + s);
                        }
                    }
                });
        way.getNodes().forEach(n -> nodeIdRenumber(n));
    }
}
