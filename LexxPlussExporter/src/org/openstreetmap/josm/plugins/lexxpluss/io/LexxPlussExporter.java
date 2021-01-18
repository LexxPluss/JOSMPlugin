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

import java.awt.Image;
import java.awt.geom.AffineTransform;
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
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.io.OsmWriterFactory;
import org.openstreetmap.josm.plugins.lexxpluss.LexxPlussUtil;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.autocalibrate.helper.GeoLine;
import org.openstreetmap.josm.plugins.piclayer.transform.PictureTransform;


import org.openstreetmap.josm.plugins.piclayer.layer.PicLayerAbstract;
import java.awt.geom.NoninvertibleTransformException;

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

        Image image = null;
        AffineTransform transform = null;
        EastNorth imagePosition = null;
        List<Layer> layers = MainApplication.getLayerManager().getVisibleLayersInZOrder();
        System.out.println("layers Count=" + layers.size());
        for(Layer _layer : layers) {
            System.out.println("layers Name=" + _layer.getName());
            System.out.println("Class Name=" + _layer.getClass().getName());
            if (_layer instanceof PicLayerAbstract) {
                System.out.println("PicLayerAbstract");
                PicLayerAbstract picLayer = (PicLayerAbstract)_layer;
                image = picLayer.getImage();
                System.out.println("Image Width=" + image.getWidth(null));
                System.out.println("Image Height=" + image.getHeight(null));
                PictureTransform transformer = picLayer.getTransformer();
                imagePosition = transformer.getImagePosition();
                System.out.println("継承Image North=" + imagePosition.north());
                System.out.println("Image East=" + imagePosition.east());
                transform = transformer.getTransform();
                System.out.println("Transform ScaleX =" + transform.getScaleX());
                System.out.println("Transform ScaleY =" + transform.getScaleY());
                System.out.println("Transform TransX =" + transform.getTranslateX());
                System.out.println("Transform TransY =" + transform.getTranslateY());
            }
            if (_layer instanceof OsmDataLayer) {
                System.out.println("OsmDataLayer");
            }
        }
        // 保存するレイヤーのデータセットを取得
        DataSet dataSet = layer.getDataSet();
        System.out.println("dataSet Count=" + dataSet.getNodes().size());
        dataSet.lock();
        List<Node> backup = new ArrayList<Node>();
        List<Node> nodes = new ArrayList<Node>(dataSet.getNodes());
        double[] srcPts = new double[nodes.size() * 2];
        double[] dstPts = new double[srcPts.length]; 
        int ofs = 0;
        for (Node node : nodes) {
            // 現在のノード情報をバックアップする\\
            backup.add(new Node(node));
            // 座標変換
            EastNorth pos = node.getEastNorth();
            pos = pos.subtract(imagePosition);
            System.out.println("Src =(" + pos.east() + "," + pos.north() + ")");
            srcPts[ofs * 2] = pos.east();
            srcPts[ofs * 2 + 1] = pos.north();
            ofs++;
        }
        try {
            transform.inverseTransform(srcPts, 0, dstPts, 0, nodes.size());
        }
        catch(java.awt.geom.NoninvertibleTransformException ex) {
            ex.printStackTrace();
        }
        ofs = 0;
        for (Node node : nodes) {
            double x = dstPts[ofs * 2];
            double y = dstPts[ofs * 2 + 1];
            System.out.println("Dst =(" + x + "," + y + ")");
            node.setEastNorth(new EastNorth(x, y));
            ofs++;
        }

        dataSet.unlock();
        // 基底クラスのファイル保存メソッドを実行する
        super.doSave(file, layer);

        // 元のノード情報をバックアップから復元する(UTM座標系→緯度経度)
        dataSet.lock();
        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).setCoor(backup.get(i).getCoor());
        }
        dataSet.unlock();
    }
}
