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
 * Action for capturing PGV tape.
 */
public class CSVPGVTapeCaptureAction extends JosmAction {

    /**
     * Constructs a new {@code CSVPGVTapeCaptureAction}.
     */
    public CSVPGVTapeCaptureAction() {
        super("Capture PGV Tape", "mapmode/amrgoalaction", "Capture PGV Tape",
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
                var agv_node_id = ToolsPlugin.getMaxId(ds.getNodes(), "agv_node_id");
                var cmds = new LinkedList<Command>();
                for (var d : data) {
                    var ne0 = transformer.imageXYtoEastNorth(d.start_x, d.start_y);
                    var ne1 = transformer.imageXYtoEastNorth(d.end_x, d.end_y);
                    var nodes = new Node[]{
                        new Node(ne0),
                        new Node(ne1)
                    };
                    nodes[0].put("agv_node_id", Integer.toString(++agv_node_id));
                    nodes[0].put("X_image", String.valueOf(d.start_x));
                    nodes[0].put("Y_image", String.valueOf(d.start_y));
                    nodes[1].put("agv_node_id", Integer.toString(++agv_node_id));
                    nodes[1].put("X_image", String.valueOf(d.end_x));
                    nodes[1].put("Y_image", String.valueOf(d.end_y));
                    var way = new Way();
                    way.addNode(nodes[0]);
                    way.addNode(nodes[1]);
                    way.put("line_info", "agv_pose");
                    way.put("agv_line_start_offset", String.valueOf(d.start_pgv));
                    way.put("agv_line_end_offset", String.valueOf(d.end_pgv));
                    cmds.add(new AddCommand(ds, nodes[0]));
                    cmds.add(new AddCommand(ds, nodes[1]));
                    cmds.add(new AddCommand(ds, way));
                }
                UndoRedoHandler.getInstance().add(new SequenceCommand("Capture PGV Tape", cmds));
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
    private Collection<PGVTapeData> loadCSVFile(File file) {
        var path = file.toPath();
        var data = new ArrayList<PGVTapeData>();
        try {
            var lines = Files.readAllLines(path);
            lines.remove(0);
            lines.forEach(line -> {
                var parts = line.split(",");
                if (parts.length >= 6) {
                    var d = new PGVTapeData();
                    d.start_x = Double.parseDouble(parts[0]);
                    d.start_y = Double.parseDouble(parts[1]);
                    d.start_pgv = Double.parseDouble(parts[2]);
                    d.end_x = Double.parseDouble(parts[3]);
                    d.end_y = Double.parseDouble(parts[4]);
                    d.end_pgv = Double.parseDouble(parts[5]);
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
     * CSV data for PGV tape.
     */
    private static class PGVTapeData {

        /**
         * Start X coordinate.
         */
        double start_x;

        /**
         * Start Y coordinate.
         */
        double start_y;

        /**
         * Start PGV value.
         */
        double start_pgv;

        /**
         * End X coordinate.
         */
        double end_x;

        /**
         * End Y coordinate.
         */
        double end_y;

        /**
         * End PGV value.
         */
        double end_pgv;

        /**
         * Constructs a new {@code PGVTapeData}.
         */
        PGVTapeData() {
            start_x = 0.0;
            start_y = 0.0;
            start_pgv = 0.0;
            end_x = 0.0;
            end_y = 0.0;
            end_pgv = 0.0;
        }
    }

}
