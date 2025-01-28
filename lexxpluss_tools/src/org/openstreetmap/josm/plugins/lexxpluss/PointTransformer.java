/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.awt.geom.AffineTransform;
import java.util.stream.Collectors;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Transforms matrix for LexxPluss.
 */
class PointTransformer {

    /**
     * The transform matrix.
     */
    private AffineTransform transform;

    /**
     * The view center longitude.
     */
    private double view_center_lon;

    /**
     * The view center latitude.
     */
    private double view_center_lat;

    /**
     * The pixel per east-north x.
     */
    private double pixel_per_en_x;

    /**
     * The pixel per east-north y.
     */
    private double pixel_per_en_y;

    /**
     * The picture offset x.
     */
    private double pic_offset_x;

    /**
     * The picture offset y.
     */
    private double pic_offset_y;

    /**
     * The scale x.
     */
    private double scaleX;

    /**
     * The scale y.
     */
    private double scaleY;

    /**
     * The half height.
     */
    private double hh;

    /**
     * The half width.
     */
    private double hw;

    /**
     * Constructs a new {@code PointTransformer}.
     */
    PointTransformer() {
        transform = null;
        view_center_lon = 0.0;
        view_center_lat = 0.0;
        pixel_per_en_x = -1.0;
        pixel_per_en_y = -1.0;
        pic_offset_x = 0.0;
        pic_offset_y = 0.0;
        scaleX = 0.0;
        scaleY = 0.0;
        hh = 0.0;
        hw = 0.0;
    }

    /**
     * Sets up the transform matrix from the given data set.
     *
     * @param dataSet the data set
     * @return {@code true} if the setup was successful, {@code false} otherwise
     */
    boolean setupFromDataSet(DataSet dataSet) {
        var way = getTransformMatrixWay(dataSet);
        if (way == null) {
            var notification = new Notification("No ways with transform matrix found.")
                    .setIcon(ImageProvider.get("data/error"));
            GuiHelper.runInEDT(notification::show);
            return false;
        }
        var matrix = new double[6];
        try {
            matrix[0] = Double.parseDouble(way.get("m0"));
            matrix[1] = Double.parseDouble(way.get("m1"));
            matrix[2] = Double.parseDouble(way.get("m2"));
            matrix[3] = Double.parseDouble(way.get("m3"));
            matrix[4] = Double.parseDouble(way.get("m4"));
            matrix[5] = Double.parseDouble(way.get("m5"));
            view_center_lon = Double.parseDouble(way.get("view_center_lon"));
            view_center_lat = Double.parseDouble(way.get("view_center_lat"));
            pixel_per_en_x = Double.parseDouble(way.get("pixel_per_en_x"));
            pixel_per_en_y = Double.parseDouble(way.get("pixel_per_en_y"));
            pic_offset_x = Double.parseDouble(way.get("pic_offset_x"));
            pic_offset_y = Double.parseDouble(way.get("pic_offset_y"));
            scaleX = Double.parseDouble(way.get("scaleX"));
            scaleY = Double.parseDouble(way.get("scaleY"));
            hw = Double.parseDouble(way.get("hw"));
            hh = Double.parseDouble(way.get("hh"));
        } catch (Exception ex) {
            var notification = new Notification("Error parsing transform matrix.")
                    .setIcon(ImageProvider.get("data/error"));
            GuiHelper.runInEDT(notification::show);
            return false;
        }
        transform = new AffineTransform(matrix);
        return true;
    }

    /**
     * Transforms the given image coordinates to east-north coordinates.
     *
     * @param x the image x coordinate
     * @param y the image y coordinate
     * @return the east-north coordinates
     */
    EastNorth imageXYtoEastNorth(double x, double y) {
        var in_point = new double[]{
                (x - hw) / scaleX + pic_offset_x / transform.getScaleX(),
                (y - hh) / scaleY + pic_offset_y / transform.getScaleY()
        };
        var cvt_point = new double[2];
        transform.transform(in_point, 0, cvt_point, 0, 1);
        var east = (cvt_point[0] / pixel_per_en_x) + view_center_lon;
        var north = view_center_lat - (cvt_point[1] / pixel_per_en_y);
        var pos = new EastNorth(east, north);
        return pos;
    }

    /**
     * Gets the way with the transform matrix.
     * @param dataSet the data set
     * @return the way with the transform matrix
     */
    static Way getTransformMatrixWay(DataSet dataSet) {
        var ways = dataSet.getWays().stream()
                .filter(w -> w.hasKey("transform matrix"))
                .collect(Collectors.toList());
        return ways.isEmpty() ? null : ways.get(0);
    }

    /**
     * Checks if the data set has a transform matrix.
     * @param dataSet the data set
     * @return {@code true} if the data set has a transform matrix, {@code false} otherwise
     */
    static boolean hasTransformMatrix(DataSet dataSet) {
        return getTransformMatrixWay(dataSet) != null;
    }
}
