/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Action for capturing AMR goal from CSV file.
 */
public class CSVAMRGoalCaptureAction extends JosmAction {

    /**
     * Constructs a new {@code CSVAMRGoalCaptureAction}.
     */
    public CSVAMRGoalCaptureAction() {
        super("Capture AMR Goal", "mapmode/amrgoalaction", "Capture AMR Goal",
                null, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var transformer = new PointTransformer();
        var ds = getLayerManager().getEditDataSet();
        if (!transformer.setupFromDataSet(ds))
            return;
        var dialog = new CSVFileDialog();
        var ret = dialog.showOpenDialog(MainApplication.getMainFrame());
        if (ret == CSVFileDialog.APPROVE_OPTION) {
            var file = dialog.getSelectedFile();
            var data = loadCSVFile(file);
            if (!data.isEmpty()) {
                var max = ToolsPlugin.getMaxId(ds.getWays(), "goal_id");
                var cmds = new LinkedList<Command>();
                for (var d : data) {
                    var ne0 = transformer.imageXYtoEastNorth(d.x, d.y);
                    var ne1 = ne0.add(2.8 * Math.cos(d.angle), -2.8 * Math.sin(d.angle));
                    var nodes = new Node[]{
                        new Node(ne0),
                        new Node(ne1)
                    };
                    nodes[0].put("X_image", String.valueOf(d.x));
                    nodes[0].put("Y_image", String.valueOf(d.y));
                    var way = new Way();
                    way.addNode(nodes[0]);
                    way.addNode(nodes[1]);
                    way.put("line_info", "goal_pose");
                    way.put("goal_id", Integer.toString(++max));
                    cmds.add(new AddCommand(ds, nodes[0]));
                    cmds.add(new AddCommand(ds, nodes[1]));
                    cmds.add(new AddCommand(ds, way));
                }
                UndoRedoHandler.getInstance().add(new SequenceCommand("Capture AMR Goal", cmds));
            }
        }
    }

    @Override
    protected void updateEnabledState() {
        var ds = getLayerManager().getEditDataSet();
        setEnabled(ds != null && PointTransformer.hasTransformMatrix(ds));
    }

    /**
     * Loads the CSV file.
     * @param file the file
     * @return the collection of CSV data
     */
    private Collection<AMRGoalData> loadCSVFile(File file) {
        var path = file.toPath();
        var data = new ArrayList<AMRGoalData>();
        try {
            var lines = Files.readAllLines(path);
            lines.remove(0);
            lines.forEach(line -> {
                var parts = line.split(",");
                if (parts.length >= 3) {
                    var d = new AMRGoalData();
                    d.x = Double.parseDouble(parts[0]);
                    d.y = Double.parseDouble(parts[1]);
                    d.angle = Double.parseDouble(parts[2]);
                    data.add(d);
                }
            });
        } catch (Exception ex) {
            var notification = new Notification("Error reading CSV file.")
                    .setIcon(ImageProvider.get("data/error"));
            GuiHelper.runInEDT(notification::show);
        }
        if (data.isEmpty()) {
            var notification = new Notification("No data found.")
                    .setIcon(ImageProvider.get("data/error"));
            GuiHelper.runInEDT(notification::show);
        }
        return data;
    }

    /**
     * CSV data for AMR goal.
     */
    private static class AMRGoalData {

        /**
         * The x-coordinate.
         */
        double x;

        /**
         * The y-coordinate.
         */
        double y;

        /**
         * The angle.
         */
        double angle;

        /**
         * Constructs a new {@code AMRGoalData}.
         */
        AMRGoalData() {
            x = 0.0;
            y = 0.0;
            angle = 0.0;
        }
    }
}
