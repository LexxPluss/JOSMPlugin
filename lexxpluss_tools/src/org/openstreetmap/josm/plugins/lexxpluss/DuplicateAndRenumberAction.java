/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.actions.AbstractPasteAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.datatransfer.PrimitiveTransferable;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action to duplicate and renumber.
 */
public class DuplicateAndRenumberAction extends AbstractPasteAction {

    /**
     * Constructs a new {@code DuplicateAndRenumberAction}.
     */
    public DuplicateAndRenumberAction() {
        super(tr("Duplicate (+Renumber)"), "duplicate", tr("Duplicate and renumber selection."),
                Shortcut.registerShortcut("system:duplicate", tr("Edit: {0}", tr("Duplicate (+Renumber)")), KeyEvent.VK_D, Shortcut.CTRL), true);
        setHelpId(ht("/Action/Duplicate"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PrimitiveTransferData data = PrimitiveTransferData.getDataWithReferences(getLayerManager().getEditDataSet().getSelected());
        doPaste(e, new PrimitiveTransferable(data));
        var ds = getLayerManager().getEditDataSet();
        var objs = ds.getSelectedNodesAndWays();
        ToolsPlugin.renumber(objs);
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledStateOnModifiableSelection(selection);
    }
}
