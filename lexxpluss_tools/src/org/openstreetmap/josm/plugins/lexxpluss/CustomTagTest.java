/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Custom tag check for LexxPluss.
 */
public class CustomTagTest extends Test {

    /**
     * Way type.
     */
    enum WayType {
        /**
         * Unknown way type.
         */
        UNKNOWN,
        /**
         * AGV pose way type.
         */
        AGV_POSE,
        /**
         * Goal pose way type.
         */
        GOAL_POSE,
        /**
         * Oneway way type.
         */
        ONEWAY
    };

    /**
     * Area type.
     */
    enum AreaType {
        /**
         * Unknown area type.
         */
        UNKNOWN,
        /**
         * Non-stop area type.
         */
        NON_STOP,
        /**
         * Parking area type.
         */
        PARK,
        /**
         * Safety area type.
         */
        SAFETY,
        /**
         * Sync area type.
         */
        SYNC
    };

    /**
     * Constructs a new {@code CustomTagTest}.
     */
    public CustomTagTest() {
        super("Custom tag check (LexxPluss)", "Custom tag check for LexxPluss");
    }

    @Override
    public void visit(Node node) {
        checkInvalidTag(node);
        checkNumericTagValue(node);
        checkTagValue(node);
        checkTagCombination(node);
        checkWayNodeCombination(node);
        var other_nodes = node.getDataSet().getNodes().stream()
                .filter(n -> n != node)
                .collect(Collectors.toList());
        Arrays.asList("agv_node_id", "intermediate_goal_id")
                .forEach(key -> checkDuplicateTagValue(node, key, other_nodes));
    }

    @Override
    public void visit(Way way) {
        checkInvalidTag(way);
        checkNumericTagValue(way);
        checkTagValue(way);
        checkTagCombination(way);
        checkWayNodeCombination(way);
        checkSplitWay(way);
        var other_ways = way.getDataSet().getWays().stream()
                .filter(w -> w != way)
                .collect(Collectors.toList());
        Arrays.asList("area_name", "goal_id", "space_id", "sync_id")
                .forEach(key -> checkDuplicateTagValue(way, key, other_ways));
    }

    /**
     * Check for invalid tag.
     * @param primitive the primitive
     */
    private void checkInvalidTag(OsmPrimitive primitive) {
        Set<String> validTags = null;
        if (primitive instanceof Node) {
            validTags = Set.of(
                    "agv_node_id",
                    "intermediate_goal_id",
                    "X_image",
                    "Y_image");
        } else if (primitive instanceof Way) {
            if (((Way)primitive).isArea()) {
                validTags = Set.of(
                        "area_base",
                        "area_detect",
                        "area_info",
                        "area_name",
                        "front_left_safety",
                        "front_right_safety",
                        "front_safety",
                        "non_stop_area",
                        "rear_left_safety",
                        "rear_right_safety",
                        "rear_safety",
                        "side_left_safety",
                        "side_right_safety",
                        "space_id",
                        "sync_id",
                        "use_scan_hi");
            } else {
                validTags = Set.of(
                        "agv_line_end_offset",
                        "agv_line_start_offset",
                        "goal_id",
                        "line_info",
                        "oneway");
            }
        }
        for (var key : primitive.keySet()) {
            if (!validTags.contains(key))
                addError(primitive, 6001, "Invalid tag:" + key);
        }
    }

    /**
     * Check for numeric tag value.
     * @param primitive the primitive
     */
    private void checkNumericTagValue(OsmPrimitive primitive) {
        var intTags = Set.of(
                "agv_node_id",
                "goal_id",
                "intermediate_goal_id",
                "space_id",
                "sync_id");
        var doubleTags = Set.of(
                "X_image",
                "Y_image");
        primitive.keySet().forEach(k -> {
            var value = primitive.get(k);
            String intValue = null;
            if (intTags.contains(k)) {
                intValue = value;
            }
            if (intValue != null) {
                try {
                    Integer.parseInt(intValue);
                } catch (NumberFormatException e) {
                    addError(primitive, 6002, "Invalid tag value:" + k + "=" + value);
                }

            }
            if (doubleTags.contains(k)) {
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    addError(primitive, 6002, "Invalid tag value:" + k + "=" + value);
                }
            }
        });
    }

    /**
     * Check for tag value.
     * @param primitive the primitive
     */
    private void checkTagValue(OsmPrimitive primitive) {
        var tagMap = Map.ofEntries(
                Map.entry("line_info",          Set.of("agv_pose", "goal_pose", "\"\"")),
                Map.entry("oneway",             Set.of("yes", "no")),
                Map.entry("area_info",          Set.of("sync_area")),
                Map.entry("area_base",          Set.of("movable")),
                Map.entry("non_stop_area",      Set.of("true")),
                Map.entry("front_safety",       Set.of("off")),
                Map.entry("front_left_safety",  Set.of("off")),
                Map.entry("front_right_safety", Set.of("off")),
                Map.entry("side_left_safety",   Set.of("off")),
                Map.entry("side_right_safety",  Set.of("off")),
                Map.entry("rear_safety",        Set.of("off")),
                Map.entry("rear_left_safety",   Set.of("off")),
                Map.entry("rear_right_safety",  Set.of("off")));
        primitive.keySet().forEach(k -> {
            var valueSet = tagMap.get(k);
            if (valueSet != null && !valueSet.contains(primitive.get(k)))
                addError(primitive, 6002, "Invalid tag value:" + k + "=" + primitive.get(k));
        });
    }

    /**
     * Check for tag combination.
     * @param primitive the primitive
     */
    private void checkTagCombination(OsmPrimitive primitive) {
        if (primitive instanceof Node) {
            checkNodeTagCombination((Node)primitive);
        } else if (primitive instanceof Way) {
            var way = (Way)primitive;
            if (way.isArea())
                checkAreaTagCombination(way);
            else
                checkWayTagCombination(way);
        }
    }

    /**
     * Check for node tag combination.
     * @param node the node
     */
    private void checkNodeTagCombination(Node node) {
        node.keySet().forEach(k -> {
            if ((k.equals("agv_node_id") && node.hasKey("intermediate_goal_id")) ||
                    (k.equals("intermediate_goal_id") && node.hasKey("agv_node_id")))
                addError(node, 6003, "Invalid tag combination agv_node_id & intermediate_goal_id");
        });
    }

    /**
     * Get way type.
     * @param way the way
     * @return the way type
     */
    private WayType getWayType(Way way) {
        for (var k : way.keySet()) {
            if (k.equals("line_info")) {
                var value = way.get(k);
                if (value.equals("agv_pose"))
                    return WayType.AGV_POSE;
                else if (value.equals("goal_pose"))
                    return WayType.GOAL_POSE;
            } else if (k.equals("goal_id")) {
                return WayType.GOAL_POSE;
            } else if (k.equals("oneway")) {
                return WayType.ONEWAY;
            }
        }
        return WayType.UNKNOWN;
    }

    /**
     * Check for way tag combination.
     * @param way the way
     */
    private void checkWayTagCombination(Way way) {
        switch (getWayType(way)) {
        case UNKNOWN:
            addError(way, 6003, "unknown way");
            break;
        case AGV_POSE:
            if (way.keySet().size() > 1)
                addError(way, 6003, "Incorrect tag number for agv pose");
            break;
        case GOAL_POSE:
            if (way.keySet().size() > 2)
                addError(way, 6003, "Incorrect tag number for goal pose");
            if (!way.hasKey("line_info") || !way.hasKey("goal_id"))
                addError(way, 6003, "Incorrect tag combination for goal pose");
            break;
        case ONEWAY:
            if (way.keySet().size() > 2)
                addError(way, 6003, "Incorrect tag number for oneway");
            if (!way.hasKey("line_info") || !way.hasKey("oneway"))
                addError(way, 6003, "Incorrect tag combination for oneway");
            break;
        }
    }

    /**
     * Get area type.
     * @param way the way
     * @return the area type
     */
    private AreaType getAreaType(Way way) {
        for (var k : way.keySet()) {
            if (k.equals("area_name")) {
                var value = way.get(k);
                if (value.equals("non stop"))
                    return AreaType.NON_STOP;
                else if (value.startsWith("park"))
                    return AreaType.PARK;
                else if (value.equals("warning"))
                    return AreaType.SAFETY;
                else if (value.startsWith("sync"))
                    return AreaType.SYNC;
            } else if (k.equals("non_stop_area")) {
                return AreaType.NON_STOP;
            } else if (k.equals("area_detect") || k.equals("space_id")) {
                return AreaType.PARK;
            } else if (k.equals("front_left_safety") || k.equals("front_right_safety") ||
                    k.equals("side_left_safety") || k.equals("side_right_safety") ||
                    k.equals("rear_left_safety") || k.equals("rear_right_safety")) {
                return AreaType.SAFETY;
            } else if (k.equals("sync_id") || k.equals("area_info")) {
                return AreaType.SYNC;
            }
        }
        return AreaType.UNKNOWN;
    }

    /**
     * Check for area tag combination.
     * @param way the way
     */
    private void checkAreaTagCombination(Way way) {
        switch (getAreaType(way)) {
        case UNKNOWN:
            addError(way, 6003, "unknown area");
            break;
        case NON_STOP:
            if (way.keySet().size() > 3)
                addError(way, 6003, "Incorrect tag number for non stop area");
            if (!way.hasKey("area_base") || !way.hasKey("area_name") ||
                    !way.hasKey("non_stop_area"))
                addError(way, 6003, "Incorrect tag combination for non stop area");
            break;
        case PARK:
            if (way.keySet().size() > 4)
                addError(way, 6003, "Incorrect tag number for parking area");
            if (!way.hasKey("area_base") || !way.hasKey("area_name") ||
                    !way.hasKey("area_detect") || !way.hasKey("space_id"))
                addError(way, 6003, "Incorrect tag combination for parking area");
            break;
        case SAFETY:
            if (way.keySet().size() > 10)
                addError(way, 6003, "Incorrect tag number for safety area");
            if (!way.hasKey("area_base") || !way.hasKey("area_name"))
                addError(way, 6003, "Incorrect tag combination for safety area");
            break;
        case SYNC:
            if (way.keySet().size() > 4)
                addError(way, 6003, "Incorrect tag number for sync area");
            if (!way.hasKey("area_base") || !way.hasKey("area_name") ||
                    !way.hasKey("sync_id") || !way.hasKey("area_info"))
                addError(way, 6003, "Incorrect tag combination for sync area");
            break;
        }
    }

    /**
     * Check for way node combination.
     * @param primitive the primitive
     */
    private void checkWayNodeCombination(OsmPrimitive primitive) {
        if (primitive instanceof Node) {
            var node = (Node)primitive;
            if (node.hasKey("agv_node_id")) {
                var ways = node.getParentWays();
                if (ways.size() != 1) {
                    addError(node, 6004, "Node with agv_node_id must be part of 1 way");
                } else {
                    var value = ways.get(0).get("line_info");
                    if (value == null || !value.equals("agv_pose"))
                        addError(node, 6004, "Node with agv_node_id must be part of way with line_info=agv_pose");
                }
            }
        } else if (primitive instanceof Way) {
            var way = (Way)primitive;
            var value = way.get("line_info");
            if (value != null) {
                if (value.equals("agv_pose")) {
                    var nodes = way.getNodes();
                    if (nodes.size() != 2)
                        addError(way, 6004, "Way with line_info=agv_pose must have 2 nodes");
                    var node0 = nodes.get(0);
                    var node1 = nodes.get(1);
                    if (!node0.hasKey("agv_node_id") || !node1.hasKey("agv_node_id"))
                        addError(way, 6004, "Way with line_info=agv_pose must have nodes with agv_node_id");
                } else if (value.equals("goal_pose")) {
                    var nodes = way.getNodes();
                    if (nodes.size() != 2)
                        addError(way, 6004, "Way with line_info=goal_pose must have 2 nodes");
                }
            }
        }
    }

    /**
     * Check for split way.
     * @param way the way
     */
    private void checkSplitWay(Way way) {
        var value = way.get("line_info");
        if (value != null && value.equals("\"\"")) {
            var oneway = way.get("oneway");
            if (oneway != null && (oneway.equals("yes") || oneway.equals("no"))) {
                if (way.getNodesCount() != 2)
                    addError(way, 6005, "Way with oneway must split");
            }
        }
    }

    /**
     * Check for duplicate IDs.
     * @param current the current primitive
     * @param key the key
     * @param other_primitives the other primitives
     */
    private void checkDuplicateTagValue(OsmPrimitive current, String key, Collection<? extends OsmPrimitive> other_primitives) {
        var value = current.get(key);
        if (value != null) {
            var values = other_primitives.stream()
                    .map(p -> p.get(key))
                    .filter(v -> v != null)
                    .collect(Collectors.toSet());
            if (values.contains(value))
                addError(current, 6006, "Duplicate tag:" + key + "=" + value);
        }
    }

    /**
     * Add an error.
     * @param primitive the primitive
     * @param number the error number
     * @param message the error message
     */
    private void addError(OsmPrimitive primitive, int number, String message) {
        errors.add(TestError.builder(this, Severity.ERROR, number)
                .message(message)
                .primitives(primitive)
                .build());
    }
}
