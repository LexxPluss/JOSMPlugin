/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.actions.AbstractPasteAction;
import org.openstreetmap.josm.gui.MainApplication;
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
        ToolsPlugin.renumber(objs);
    }
}
