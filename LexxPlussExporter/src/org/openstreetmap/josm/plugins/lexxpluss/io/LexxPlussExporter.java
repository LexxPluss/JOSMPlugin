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


import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.OsmExporter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.plugins.piclayer.layer.PicLayerAbstract;
import org.openstreetmap.josm.plugins.piclayer.transform.PictureTransform;
import org.openstreetmap.josm.spi.preferences.Config;
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
    private static final String EXTENSION = "osm";
    
    public LexxPlussExporter() {
        super(new ExtensionFileFilter(EXTENSION, EXTENSION, 
                tr("OSM Server Files LexxPluss format") + " (*."+EXTENSION+")"));
    }

    @Override
    protected void doSave(File file, OsmDataLayer layer) throws IOException {

        // record info in this tag to save for re-transform
        TagMap tags = new TagMap();
        tags.put("transform matrix","");

        Image image = null;
        AffineTransform transform = null;   // 画像の表示位置変換アフィン行列
        EastNorth imagePosition = null;     // en単位での画像中央位置
        PicLayerAbstract picLayer = null;
        // PicLayerを検索し、情報を取得
        List<Layer> layers = MainApplication.getLayerManager().getVisibleLayersInZOrder();
        // System.out.println("layers Count=" + layers.size());
        final MapFrame mf = MainApplication.getMap();
        MapView mv = mf.mapView;
        EastNorth center = mv.getCenter();
        EastNorth leftop = mv.getEastNorth(0, 0);
        double pixel_per_en_x = (mv.getWidth() / 2.0) / (center.east() - leftop.east());  // 1en当たりのピクセル数
        //System.out.println("pixel_per_en_x=" + pixel_per_en_x);
        double pixel_per_en_y = (mv.getHeight() / 2.0) / (leftop.north() - center.north());  // 1en当たりのピクセル数
        //System.out.println("pixel_per_en_y=" + pixel_per_en_y);

        tags.put("view_center_lon", Double.valueOf(center.east()).toString());
        tags.put("view_center_lat", Double.valueOf(center.north()).toString());
        tags.put("pixel_per_en_x",Double.valueOf(pixel_per_en_x).toString());
        tags.put("pixel_per_en_y",Double.valueOf(pixel_per_en_y).toString());

        double pic_offset_x = 0, pic_offset_y = 0;  // ピクセル単位の画像中央位置
        // This is now the offset in screen pixels
        for(Layer _layer : layers) {
            // System.out.println("layers Name=" + _layer.getName());
            // System.out.println("Class Name=" + _layer.getClass().getName());
            if (_layer instanceof PicLayerAbstract) {
                // System.out.println("PicLayerAbstract");
                picLayer = (PicLayerAbstract)_layer;
                image = picLayer.getImage();
                PictureTransform transformer = picLayer.getTransformer();
                transform = transformer.getTransform(); 
                imagePosition = transformer.getImagePosition();
                pic_offset_x = ((imagePosition.east() - center.east()) * pixel_per_en_x);
                pic_offset_y = ((center.north() - imagePosition.north()) * pixel_per_en_y);
                
                tags.put("pic_offset_x",Double.valueOf(pic_offset_x).toString());
                tags.put("pic_offset_y",Double.valueOf(pic_offset_y).toString());

                /*
                System.out.println("pixel_per_en=" + pixel_per_en);
                System.out.println("center North=" + center.north());
                System.out.println("center East=" + center.east());
                System.out.println("picture offset x=" + pic_offset_x);
                System.out.println("picture offset y=" + pic_offset_y);
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
            // 通常のOSM保存処理を実行させる
            super.doSave(file, layer);
            // PicLayerが見つからない場合はエラー
            //JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("PicLayer is not existed."));
            return;
        }
        // 保存するレイヤーのデータセットを取得
        DataSet dataSet = layer.getDataSet();
        //System.out.println("dataSet Count=" + dataSet.getNodes().size());
        //dataSet.lock();
        dataSet.beginUpdate();
         
        List<Node> nodes = new ArrayList<Node>(dataSet.getNodes());
        double[] srcPts = new double[nodes.size() * 2];
        double[] dstPts = new double[srcPts.length];
        // 座標変換準備
        int ofs = 0;
        double initialImageScale = getInitialImageScale(picLayer);
        for (Node node : nodes) {
            EastNorth pos = node.getEastNorth();
            srcPts[ofs * 2] = (pos.east() - center.east()) * pixel_per_en_x;
            // 画像座標系と地図座標系ではY軸の方向が逆
            srcPts[ofs * 2 + 1] =  (center.north() - pos.north()) * pixel_per_en_y;
            // srcPts[ofs * 2] = ((pos.east() - center.east()) * (mv.getWidth() / 2.0)) / (center.east() - leftop.east());
            // srcPts[ofs * 2 + 1] =  ((center.north() - pos.north()) * (mv.getWidth() / 2.0)) / (center.east() - leftop.east());
            //System.out.println("Src =(" + srcPts[ofs * 2] + "," + srcPts[ofs * 2 + 1] + ")");
            ofs++;
        }
        // アフィン行列逆変換 
        try {
            double[] matrix = new double[6];
            transform.getMatrix(matrix);
            matrix[4] = 0.0;    // アフィン行列内の平行移動要素を消去
            matrix[5] = 0.0;
            transform = new AffineTransform(matrix);
            /*
            System.out.println("Transform ScaleX =" + transform.getScaleX());
            System.out.println("Transform ScaleY =" + transform.getScaleY());
            System.out.println("Transform TransX =" + transform.getTranslateX());
            System.out.println("Transform TransY =" + transform.getTranslateY());
            System.out.println("Transform ShearX =" + transform.getShearX());
            System.out.println("Transform ShearY =" + transform.getShearY());
            */
            transform.inverseTransform(srcPts, 0, dstPts, 0, nodes.size());
            
           // save matrix in the tags
            tags.put("m0",Double.valueOf(matrix[0]).toString());
            tags.put("m1",Double.valueOf(matrix[1]).toString());
            tags.put("m2",Double.valueOf(matrix[2]).toString());
            tags.put("m3",Double.valueOf(matrix[3]).toString());
            tags.put("m4",Double.valueOf(matrix[4]).toString());
            tags.put("m5",Double.valueOf(matrix[5]).toString());
            
        } catch (NoninvertibleTransformException e) {
            Logging.log(Level.WARNING, "Could not inverseTransform.", e);
            // 通常のOSM保存処理を実行させる
            super.doSave(file, layer);
            return;
        }
        // スケール調整
        ofs = 0;
        try {
            // System.out.println("getMetersPerEasting=" + getMetersPerEasting(picLayer, imagePosition));
            // System.out.println("getMetersPerNorthing=" + getMetersPerNorthing(picLayer, imagePosition));
            // 画像半縦幅、半横幅
            double hw = image.getWidth(null) / 2.0;
            double hh = image.getHeight(null) / 2.0;
            // スケール補正値
            double scaleX = (100.0 * getMetersPerEasting(picLayer, imagePosition)) / (initialImageScale * pixel_per_en_x);
            double scaleY = (100.0 * getMetersPerNorthing(picLayer, imagePosition)) / (initialImageScale * pixel_per_en_y);
            // System.out.println("scaleX=" + scaleX);
            // System.out.println("scaleY=" + scaleY);

            tags.put("hw", Double.valueOf(hw).toString());
            tags.put("hh", Double.valueOf(hh).toString());
            tags.put("scaleX", Double.valueOf(scaleX).toString());
            tags.put("scaleY", Double.valueOf(scaleY).toString());
            
            for (Node node : nodes) {
                double x = dstPts[ofs * 2];
                double y = dstPts[ofs * 2 + 1];
                // System.out.println("Dst1 =(" + x + "," + y + ")");
                // node.setEastNorth(new EastNorth(x, y));
                x -= pic_offset_x / transform.getScaleX();
                y -= pic_offset_y / transform.getScaleY();
                // System.out.println("Dst2 =(" + x + "," + y + ")");
                x *= scaleX;
                y *= scaleY;
                // System.out.println("Dst3 =(" + x + "," + y + ")");
                x = hw + x;
                y = hh + y;
                // 浮動小数点による誤差への対処
                x = Math.floor(x * 1000000) / 1000000;
                y = Math.floor(y * 1000000) / 1000000;
                //System.out.println("Dst4 =(" + x + "," + y + ")");
                // 変換した座標をタグにセット
                // System.out.println("Update Keys=" + node.getNumKeys());
                node.put("X_image", String.valueOf(x));
                node.put("Y_image", String.valueOf(y));
                // System.out.println("Node Keys=" + node.getNumKeys());
                ofs++;
            }
        } catch (Exception e) {
            Logging.log(Level.WARNING, "Could not rescaling.", e);
            // 通常のOSM保存処理を実行させる
            super.doSave(file, layer);
            return;
        }
        try {
			List<Way> ways = new ArrayList<Way>(dataSet.getWays());
			boolean exist = false;
			for (Way way: ways) {
				TagMap ts = way.getKeys();
				for (Tag tag: ts.getTags()) {
					if (tag.getKey() == "transform matrix") {
						exist = true;
						way.removeAll();
						way.setKeys(tags);
						break;
					}
				}
				if (exist) {
					break;
				}
			}
			if (!exist) {
				// create new Way
				Way w = new Way();
				w.setKeys(tags);
				dataSet.addPrimitive(w);
			}
        }
        catch (Exception e) {
        	// do nothing
        }
        dataSet.endUpdate();
        //dataSet.unlock();
        // 基底クラスのファイル保存メソッドを実行する
        super.doSave(file, layer);
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
