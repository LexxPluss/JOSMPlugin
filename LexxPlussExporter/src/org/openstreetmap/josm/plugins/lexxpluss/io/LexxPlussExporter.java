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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
        PicLayerAbstract picLayer = null;
        List<Layer> layers = MainApplication.getLayerManager().getVisibleLayersInZOrder();
        //System.out.println("layers Count=" + layers.size());
        for(Layer _layer : layers) {
            //System.out.println("layers Name=" + _layer.getName());
            //System.out.println("Class Name=" + _layer.getClass().getName());
            if (_layer instanceof PicLayerAbstract) {
                //System.out.println("PicLayerAbstract");
                picLayer = (PicLayerAbstract)_layer;
                image = picLayer.getImage();
                System.out.println("Image Width=" + image.getWidth(null));
                System.out.println("Image Height=" + image.getHeight(null));
                PictureTransform transformer = picLayer.getTransformer();
                imagePosition = transformer.getImagePosition();
                System.out.println("Image North=" + imagePosition.north());
                System.out.println("Image East=" + imagePosition.east());
                transform = transformer.getTransform();
                System.out.println("Transform ScaleX =" + transform.getScaleX());
                System.out.println("Transform ScaleY =" + transform.getScaleY());
                System.out.println("Transform TransX =" + transform.getTranslateX());
                System.out.println("Transform TransY =" + transform.getTranslateY());
                System.out.println("InitialImageScale =" + getInitialImageScale(picLayer));
            }
            if (_layer instanceof OsmDataLayer) {
                //System.out.println("OsmDataLayer");
            }
        }
        // 保存するレイヤーのデータセットを取得
        DataSet dataSet = layer.getDataSet();
        //System.out.println("dataSet Count=" + dataSet.getNodes().size());
        dataSet.lock();
        List<Node> backup = new ArrayList<Node>();
        List<Node> nodes = new ArrayList<Node>(dataSet.getNodes());
        double[] srcPts = new double[nodes.size() * 2];
        double[] dstPts = new double[srcPts.length]; 
        for (Node node : nodes) {
            // 座標変換
            EastNorth pos = node.getEastNorth();
            System.out.println("EastNorth =(" + pos.east() + "," + pos.north() + ")");
            LatLon latlon = node.getCoor();
            System.out.println("LatLon =(" + latlon.lon() + "," + latlon.lat() + ")");
        }
        int ofs = 0;
        for (Node node : nodes) {
            // 現在のノード情報をバックアップする
            backup.add(new Node(node));
            // 座標変換
            EastNorth pos = node.getEastNorth();
            srcPts[ofs * 2] = pos.east() - imagePosition.east();
            srcPts[ofs * 2 + 1] = pos.north() - imagePosition.north();
            System.out.println("Src =(" + srcPts[ofs * 2] + "," + srcPts[ofs * 2 + 1] + ")");
            ofs++;
        }
        try {
            transform.inverseTransform(srcPts, 0, dstPts, 0, nodes.size());
        }
        catch(java.awt.geom.NoninvertibleTransformException ex) {
            ex.printStackTrace();
        }
        ofs = 0;
        double initialImageScale = getInitialImageScale(picLayer);
        double hw = image.getWidth(null) / 2.0;
        double hh = image.getHeight(null) / 2.0;
        //System.out.println("ImageHafeSize =(" + hw + "," + hh + ")");
        for (Node node : nodes) {
            //System.out.println("DstPts =(" + dstPts[ofs * 2] + "," + dstPts[ofs * 2 + 1] + ")");
            //double x = dstPts[ofs * 2]  * 100.0 /  initialImageScale / 2.0837706894099863 * 0.8214007020307434 + hw;
            //double y = dstPts[ofs * 2 + 1] * 100.0 / initialImageScale / 2.0837706894099863 * 0.8214007118841223 + hh;
            //double x = dstPts[ofs * 2]  * 100.0 /  initialImageScale * 0.8214007020307434 + hw;
            //double y = dstPts[ofs * 2 + 1] * 100.0 / initialImageScale * 0.8214007118841223 + hh;
            double x = dstPts[ofs * 2]  * 100.0 /  initialImageScale * getMetersPerEasting(picLayer, imagePosition) + hw;
            double y = dstPts[ofs * 2 + 1] * 100.0 / initialImageScale * getMetersPerNorthing(picLayer, imagePosition) + hh;
            System.out.println("Dst =(" + x + "," + y + ")");
            node.setCoor(new LatLon(x, y));
            ofs++;
        }

        dataSet.unlock();
        // 基底クラスのファイル保存メソッドを実行する
        super.doSave(file, layer);

        // 元のノード情報をバックアップから復元する
        dataSet.lock();
        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).setCoor(backup.get(i).getCoor());
        }
        dataSet.unlock();
    }

    private double getInitialImageScale(PicLayerAbstract picLayer) {
        double r = 0.0;
        try {
            // PicLayerAbstractは抽象クラスなので子クラスから親クラスを参照する
            Field f = picLayer.getClass().getSuperclass().getDeclaredField("initialImageScale");
            f.setAccessible(true);
            r = (double)f.get(picLayer);
        }
        catch (NoSuchFieldException nsfe) {
            nsfe.printStackTrace();
        }
        catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }
        return r;
    }

    private double getMetersPerEasting(PicLayerAbstract picLayer, EastNorth en) {
        double r = 0.0;
        try {
            // PicLayerAbstractは抽象クラスなので子クラスから親クラスを参照する
            Method m = picLayer.getClass().getSuperclass().getDeclaredMethod("getMetersPerEasting", org.openstreetmap.josm.data.coor.EastNorth.class);
            m.setAccessible(true);
            r = (double)m.invoke(picLayer, en); 
        }
        catch (NoSuchMethodException nsme) {
            nsme.printStackTrace();
        }
        catch (InvocationTargetException ite) {
            ite.printStackTrace();
        }
        catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }
        return r;
    }

    private double getMetersPerNorthing(PicLayerAbstract picLayer, EastNorth en) {
        double r = 0.0;
        try {
            // PicLayerAbstractは抽象クラスなので子クラスから親クラスを参照する
            Method m = picLayer.getClass().getSuperclass().getDeclaredMethod("getMetersPerNorthing", org.openstreetmap.josm.data.coor.EastNorth.class);
            m.setAccessible(true);
            r = (double)m.invoke(picLayer, en); 
        }
        catch (NoSuchMethodException nsme) {
            nsme.printStackTrace();
        }
        catch (InvocationTargetException ite) {
            ite.printStackTrace();
        }
        catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }
        return r;
    }
}
