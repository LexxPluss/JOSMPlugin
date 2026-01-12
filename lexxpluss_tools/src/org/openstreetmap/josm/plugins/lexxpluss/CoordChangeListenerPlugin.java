/**
 * Copyright (c) 2025, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.geom.AffineTransform;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.awt.Image;
import java.util.List;
import java.util.Map;
import java.awt.geom.NoninvertibleTransformException;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.piclayer.layer.PicLayerAbstract;
import org.openstreetmap.josm.plugins.piclayer.transform.PictureTransform;
import org.openstreetmap.josm.tools.Logging;

/**
 * Action to set the node coords automatically
 */
public class CoordChangeListenerPlugin implements DataSetListener {

    OsmDataLayer currentLayer;
    LatLon finalLatLon;

    /**
     * Constructs a new {@code CoordChangeListenerPlugin}.
     */
    public CoordChangeListenerPlugin() {
        super();
        MainApplication.getLayerManager().addActiveLayerChangeListener(e -> {
            OsmDataLayer newLayer = MainApplication.getLayerManager().getEditLayer();
            if (newLayer != null) {
                newLayer.data.addDataSetListener(this);
            }
        });

        OsmDataLayer currentLayer = MainApplication.getLayerManager().getActiveDataLayer();
        if (currentLayer != null) {
            currentLayer.data.addDataSetListener(this);
        }
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        for (OsmPrimitive primitive : event.getPrimitives()) {
            if (!(primitive instanceof Node)) {
                continue;
            }
            Node node = (Node) primitive;
            handleNodeTagChange(node, event);
            SwingUtilities.invokeLater(() -> {
                performCoordinateUpdate(node);
            });
        }
    }


    private void handleNodeTagChange(Node node, TagsChangedEvent event) {
        // Get the original tags (before change)
        Map<String, String> originalTags = event.getOriginalKeys();
        Map<String, String> newTags = node.getKeys();

        if (originalTags.get("X_image").isEmpty() || originalTags.get("Y_image").isEmpty()) {
            return;
        }

        // Check what changed
        for (Map.Entry<String, String> entry : newTags.entrySet()) {
            String key = entry.getKey();
            String newValue = entry.getValue();
            String oldValue = originalTags.get(key);

            if (oldValue != null && !oldValue.equals(newValue) && ("X_image".equals(key) || "Y_image".equals(key))) {
                onNodeTagChanged(node, originalTags, newTags, key);
            }
        }
    }

    private void onNodeTagChanged(Node node, Map<String, String> originalTags, Map<String, String> newTags, String tagThatHasChanged) {
        if ("X_image".equals(tagThatHasChanged)) {
            moveNodeToCustomCoordinates(node, Double.parseDouble(newTags.get(tagThatHasChanged)), Double.parseDouble(newTags.get("Y_image")));
        } else if ("Y_image".equals(tagThatHasChanged)) {
            moveNodeToCustomCoordinates(node, Double.parseDouble(newTags.get("X_image")), Double.parseDouble(newTags.get(tagThatHasChanged)));
        }
    }

    private void moveNodeToCustomCoordinates(Node node, double customX, double customY) {
        // Reverse the transformation steps in opposite order
        final MapFrame mf = MainApplication.getMap();
        MapView mv = mf.mapView;
        Image image = null;
        AffineTransform transform = null;
        EastNorth imagePosition = null;
        PicLayerAbstract picLayer = null;
        EastNorth center = mv.getCenter();
        EastNorth leftop = mv.getEastNorth(0, 0);
        double pixel_per_en_x = (mv.getWidth() / 2.0) / (center.east() - leftop.east());
        double pixel_per_en_y = (mv.getHeight() / 2.0) / (leftop.north() - center.north());

        List<Layer> layers = MainApplication.getLayerManager().getVisibleLayersInZOrder();
        double pic_offset_x = 0, pic_offset_y = 0;
        for (Layer _layer : layers) {
            if (_layer instanceof PicLayerAbstract) {
                picLayer = (PicLayerAbstract)_layer;
                image = picLayer.getImage();
                PictureTransform transformer = picLayer.getTransformer();
                transform = transformer.getTransform(); 
                imagePosition = transformer.getImagePosition();
                pic_offset_x = ((imagePosition.east() - center.east()) * pixel_per_en_x);
                pic_offset_y = ((center.north() - imagePosition.north()) * pixel_per_en_y);
            }
        }

        if (picLayer == null || image == null || transform == null) {
            return;
        }

        EastNorth pos = node.getEastNorth();
        double[] srcPts = new double[2];
        double[] dstPts = new double[srcPts.length];

        srcPts[0] = (pos.east() - center.east()) * pixel_per_en_x;
        srcPts[1] =  (center.north() - pos.north()) * pixel_per_en_y;

        double initialImageScale = getInitialImageScale(picLayer);
        PictureTransform transformer = picLayer.getTransformer();
        transform = transformer.getTransform();
        double[] matrix = new double[6];
        transform.getMatrix(matrix);
        matrix[4] = 0.0;
        matrix[5] = 0.0;
        transform = new AffineTransform(matrix);
        try {
            transform.inverseTransform(srcPts, 0, dstPts, 0, 1);
        }
        catch (NoninvertibleTransformException e) {
            Logging.log(Level.WARNING, "Could not inverseTransform.", e);
            return;
        }

        double hw = image.getWidth(null) / 2.0;
        double hh = image.getHeight(null) / 2.0;

        double scaleX = (100.0 * getMetersPerEasting(picLayer, imagePosition)) / (initialImageScale * pixel_per_en_x);
        double scaleY = (100.0 * getMetersPerNorthing(picLayer, imagePosition)) / (initialImageScale * pixel_per_en_y);

        // Step 1: Reverse the floor operation (already done, customX and customY are the floored values)
        double x = customX;
        double y = customY;

        // Step 2: Reverse the half-width/height addition
        x = x - hw;
        y = y - hh;

        // Step 3: Reverse the scaling
        x = x / scaleX;
        y = y / scaleY;

        // Step 4: Reverse the picture offset
        x = x + (pic_offset_x / transform.getScaleX());
        y = y + (pic_offset_y / transform.getScaleY());

        double[] imgPt = { x, y };
        double[] mapPt = new double[2];
        transform.transform(imgPt, 0, mapPt, 0, 1);

        double east = (mapPt[0] / pixel_per_en_x) + center.east();
        double north = center.north() - (mapPt[1] / pixel_per_en_y);

        Projection projection = ProjectionRegistry.getProjection();

        // Convert to LatLon
        this.finalLatLon = projection.eastNorth2latlon(new EastNorth(east, north));
    }

    private void performCoordinateUpdate(Node node) {
//        System.out.println("Node shall be moved to map coords: " + Double.toString(finalLatLon.getX()) + ", " + Double.toString(finalLatLon.getY()));
        UndoRedoHandler.getInstance().add(new MoveCommand(node, this.finalLatLon));
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
            Method m = picLayer.getClass().getSuperclass().getDeclaredMethod("getMetersPerNorthing", EastNorth.class);
            m.setAccessible(true);
            r = (double)m.invoke(picLayer, en); 
        }
        catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Logging.log(Level.WARNING, "Could not invoke PicLayerAbstract.getMetersPerNorthing.", e);
        }
        return r;
    }


    // Other DataSetListener methods (required by interface)
    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {}

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {}

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {}

    @Override
    public void nodeMoved(NodeMovedEvent event) {}

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event) {}

    @Override
    public void dataChanged(DataChangedEvent event) {}

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {}
}
