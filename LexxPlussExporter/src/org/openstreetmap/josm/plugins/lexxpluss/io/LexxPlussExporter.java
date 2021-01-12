// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.lexxpluss.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.io.importexport.OsmExporter;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.CachedFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.InvalidPathException;
import java.text.MessageFormat;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.io.OsmWriterFactory;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * OSM Exporter for o5n format (*.osn).
 * @author LexxPluss
 *
 */
public class LexxPlussExporter extends OsmExporter {
    /**
     * File extension.
     */
    private static final String EXTENSION = "osn";
    
    public LexxPlussExporter() {
        super(new ExtensionFileFilter(EXTENSION, EXTENSION, 
                tr("OSM Server Files LexxPluss format") + " (*."+EXTENSION+")"));
    }

    @Override
    protected void doSave(File file, OsmDataLayer layer) throws IOException {

        DataSet dataSet = layer.getDataSet();
        System.out.println("dataSet Count=" + dataSet.getNodes().size());
        dataSet.lock();
        List<Node> backup = new ArrayList<Node>();
        Collection<Node> cnodes = dataSet.getNodes();
        List<Node> nodes = new ArrayList<Node>(cnodes);
        for (Node node : nodes) {
            // 現在のノード情報をバックアップする
            backup.add(new Node(node));
            //
            // TODO 座標変換
            //
            node.setCoor(new LatLon(0.0, 0.0));
        }
        dataSet.unlock();
        super.doSave(file, layer);

        // 元のノード情報を復元する
        dataSet.lock();
        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).setCoor(backup.get(i).getCoor());
        }
        dataSet.unlock();
    }
}
