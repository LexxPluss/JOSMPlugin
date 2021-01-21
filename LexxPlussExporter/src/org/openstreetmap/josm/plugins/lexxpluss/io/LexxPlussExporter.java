// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.lexxpluss.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.OsmExporter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.piclayer.layer.PicLayerAbstract;
import org.openstreetmap.josm.plugins.piclayer.transform.PictureTransform;
import org.openstreetmap.josm.tools.Logging;

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
        // PicLayerを検索し、情報を取得
        List<Layer> layers = MainApplication.getLayerManager().getVisibleLayersInZOrder();
        //System.out.println("layers Count=" + layers.size());
        for(Layer _layer : layers) {
            //System.out.println("layers Name=" + _layer.getName());
            //System.out.println("Class Name=" + _layer.getClass().getName());
            if (_layer instanceof PicLayerAbstract) {
                //System.out.println("PicLayerAbstract");
                picLayer = (PicLayerAbstract)_layer;
                image = picLayer.getImage();
                PictureTransform transformer = picLayer.getTransformer();
                imagePosition = transformer.getImagePosition();
                transform = transformer.getTransform();
                /*
                System.out.println("Image Width=" + image.getWidth(null));
                System.out.println("Image Height=" + image.getHeight(null));
                System.out.println("Image North=" + imagePosition.north());
                System.out.println("Image East=" + imagePosition.east());
                System.out.println("Transform ScaleX =" + transform.getScaleX());
                System.out.println("Transform ScaleY =" + transform.getScaleY());
                System.out.println("Transform TransX =" + transform.getTranslateX());
                System.out.println("Transform TransY =" + transform.getTranslateY());
                System.out.println("InitialImageScale =" + getInitialImageScale(picLayer));
                */
            }
            /*
            if (_layer instanceof OsmDataLayer) {
                System.out.println("OsmDataLayer");
            }
            */
        }
        if (picLayer == null || image == null || transform == null) {
            // PicLayerが見つからない場合はエラー
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("PicLayer is not existed."));
            return;
        }
        // 保存するレイヤーのデータセットを取得
        DataSet dataSet = layer.getDataSet();
        //System.out.println("dataSet Count=" + dataSet.getNodes().size());
        dataSet.lock();
        List<Node> backup = new ArrayList<Node>();
        List<Node> nodes = new ArrayList<Node>(dataSet.getNodes());
        double[] srcPts = new double[nodes.size() * 2];
        double[] dstPts = new double[srcPts.length];
        /*
        for (Node node : nodes) {
            // 座標表示
            EastNorth pos = node.getEastNorth();
            System.out.println("EastNorth =(" + pos.east() + "," + pos.north() + ")");
            LatLon latlon = node.getCoor();
            System.out.println("LatLon =(" + latlon.lon() + "," + latlon.lat() + ")");
        }
        */
        // 座標変換準備
        int ofs = 0;
        for (Node node : nodes) {
            // 現在のノード情報をバックアップする
            backup.add(new Node(node));
            EastNorth pos = node.getEastNorth();
            srcPts[ofs * 2] = pos.east() - imagePosition.east();
            // 画像座標系と地図座標系ではY軸の方向が逆
            srcPts[ofs * 2 + 1] =  - pos.north() + imagePosition.north();
            System.out.println("Src =(" + srcPts[ofs * 2] + "," + srcPts[ofs * 2 + 1] + ")");
            ofs++;
        }
        // アフィン行列逆変換 
        try {
            transform.inverseTransform(srcPts, 0, dstPts, 0, nodes.size());
        } catch (NoninvertibleTransformException e) {
            Logging.log(Level.WARNING, "Could not inverseTransform.", e);
            return;
        }
        // スケール調整
        ofs = 0;
        try {
            double initialImageScale = getInitialImageScale(picLayer);
            double scaleX = 100.0 /  initialImageScale * getMetersPerEasting(picLayer, imagePosition);
            double scaleY = 100.0 / initialImageScale * getMetersPerNorthing(picLayer, imagePosition);
            double hw = image.getWidth(null) / 2.0;
            double hh = image.getHeight(null) / 2.0;
            for (Node node : nodes) {
                double x = dstPts[ofs * 2]  * scaleX + hw;
                double y = dstPts[ofs * 2 + 1] * scaleY + hh;
                System.out.println("Dst =(" + x + "," + y + ")");
                // 変換した座標をノードに再セット
                node.setCoor(new LatLon(x, y));
                ofs++;
            }
        } catch (Exception e) {
            Logging.log(Level.WARNING, "Could not rescaling.", e);
            return;
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

    /**
     * get PicLayerAbstract.initialImageScale
     * @param picLayer PicLayerAbstract instance
     * @return PicLayerAbstract.initialImageScale
     */
    private double getInitialImageScale(PicLayerAbstract picLayer) {
        double r = Double.NaN;
        try {
            // PicLayerAbstractは抽象クラスなのでpicLayerの実態は必ず継承クラスになる。
            // リファクタリングには継承クラスから親クラスのPicLayerAbstractを参照する
            Field f = picLayer.getClass().getSuperclass().getDeclaredField("initialImageScale");
            f.setAccessible(true);
            r = (double)f.get(picLayer);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            Logging.log(Level.WARNING, "Could not get PicLayerAbstract.initialImageScale.", e);
        }
        return r;
    }

    /**
     * get MetersPerEasting
     * @param picLayer PicLayerAbstract instance
     * @param en imagePosition
     * @return MetersPerEasting
     */
    private double getMetersPerEasting(PicLayerAbstract picLayer, EastNorth en) {
        double r = Double.NaN;
        try {
            // PicLayerAbstractは抽象クラスなのでpicLayerの実態は必ず継承クラスになる。
            // リファクタリングには継承クラスから親クラスのPicLayerAbstractを参照する
            // 引数の型は必ず指定する
            Method m = picLayer.getClass().getSuperclass().getDeclaredMethod("getMetersPerEasting", EastNorth.class);
            m.setAccessible(true);
            r = (double)m.invoke(picLayer, en); 
        }
        catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Logging.log(Level.WARNING, "Could not invoke PicLayerAbstract.getMetersPerEasting.", e);
        }
        return r;
    }

    /**
     * get MetersPerNorthing
     * @param picLayer PicLayerAbstract instance
     * @param en imagePosition
     * @return MetersPerNorthing
     */
    private double getMetersPerNorthing(PicLayerAbstract picLayer, EastNorth en) {
        double r = Double.NaN;
        try {
            // PicLayerAbstractは抽象クラスなのでpicLayerの実態は必ず継承クラスになる。
            // リファクタリングには継承クラスから親クラスのPicLayerAbstractを参照する
            // 引数の型は必ず指定する
            Method m = picLayer.getClass().getSuperclass().getDeclaredMethod("getMetersPerNorthing", EastNorth.class);
            m.setAccessible(true);
            r = (double)m.invoke(picLayer, en); 
        }
        catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Logging.log(Level.WARNING, "Could not invoke PicLayerAbstract.getMetersPerNorthing.", e);
        }
        return r;
    }
}
