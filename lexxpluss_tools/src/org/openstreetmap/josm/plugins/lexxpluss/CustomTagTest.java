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
        Arrays.asList("goal_id", "space_id", "sync_id", "area_name")
                .forEach(key -> checkDuplicateTagValue(way, key, other_ways));
    }

    /**
     * Check for invalid tag.
     * @param primitive the primitive
     */
    private void checkInvalidTag(OsmPrimitive primitive) {
        Set<String> validTags = null;
        if (primitive instanceof Node) {
            validTags = Set.of("agv_node_id", "intermediate_goal_id", "X_image", "Y_image");
        } else if (primitive instanceof Way) {
            if (((Way)primitive).isArea()) {
                validTags = Set.of("space_id", "sync_id",
                        "area_base", "area_detect", "area_info", "area_name", "no_stop_area",
                        "front_left_safety", "front_right_safety",
                        "side_left_safety", "side_right_safety",
                        "rear_left_safety", "rear_right_safety");
            } else {
                validTags = Set.of("goal_id", "line_info", "oneway");
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
                "agv_node_id", "goal_id",
                "space_id", "sync_id",
                "intermediate_goal_id");
        var doubleTags = Set.of("X_image", "Y_image");
        primitive.keySet().forEach(k -> {
            var value = primitive.get(k);
            String intValue = null;
            if (intTags.contains(k)) {
                intValue = value;
            } else if (k.equals("area_name") &&
                    (value.startsWith("park") || value.startsWith("sync"))) {
                intValue = value.substring(4);
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
                Map.entry("one_way",            Set.of("yes", "no")),
                Map.entry("area_info",          Set.of("sync_area")),
                Map.entry("area_base",          Set.of("movable")),
                Map.entry("no_stop_area",       Set.of("true")),
                Map.entry("front_left_safety",  Set.of("off")),
                Map.entry("front_right_safety", Set.of("off")),
                Map.entry("side_left_safety",   Set.of("off")),
                Map.entry("side_right_safety",  Set.of("off")),
                Map.entry("rear_left_safety",   Set.of("off")),
                Map.entry("rear_right_safety",  Set.of("off")));
        primitive.keySet().forEach(k -> {
            var valueSet = tagMap.get(k);
            if (valueSet != null && !valueSet.contains(primitive.get(k)))
                addError(primitive, 6002, "Invalid tag value:" + k + "=" + primitive.get(k));
            if (k.equals("area_name")) {
                var value = primitive.get(k);
                if (value != null) {
                    if (!value.equals("warning") && !value.equals("no stop") &&
                            !value.startsWith("park") && !value.startsWith("sync"))
                        addError(primitive, 6002, "Invalid tag value:" + k + "=" + value);
                }
            }
        });
    }

    /**
     * Check for tag combination.
     * @param primitive the primitive
     */
    private void checkTagCombination(OsmPrimitive primitive) {
        var keySet = new HashSet<>(primitive.keySet());
        if (primitive instanceof Node) {
            primitive.keySet().forEach(k -> {
                if ((k.equals("agv_node_id") && keySet.contains("intermediate_goal_id")) ||
                        (k.equals("intermediate_goal_id") && keySet.contains("agv_node_id")))
                    addError(primitive, 6003, "Invalid tag combination agv_node_id & intermediate_goal_id");
            });
        } else if (primitive instanceof Way) {
            if (((Way)primitive).isArea()) {
                primitive.keySet().forEach(k -> {
                    if (k.equals("area_base")) {
                    }
                });
            } else {
                primitive.keySet().forEach(k -> {
                    if (k.equals("oneway")) {
                        if (keySet.size() > 2)
                            addError(primitive, 6003, "Invalid tag combination oneway & other tags");
                        if (!keySet.contains("line_info"))
                            addError(primitive, 6003, "Tag oneway needs line_info");
                    }
                    if (k.equals("line_info")) {
                        var value = primitive.get(k);
                        if (value.equals("agv_pose") && keySet.size() > 1)
                            addError(primitive, 6003, "Invalid tag combination line_info=agv_pose & other tags");
                        if (value.equals("goal_pose")) {
                            if (keySet.size() > 2)
                                addError(primitive, 6003, "Invalid tag combination line_info=goal_pose & other tags");
                            if (!keySet.contains("goal_id"))
                                addError(primitive, 6003, "Tag line_info=goal_pose needs goal_id");
                        }
                        if (value.equals("\"\"") && !keySet.contains("oneway"))
                            addError(primitive, 6003, "Tag line_info=\"\" needs oneway");
                    }
                });
            }
        }
    }

    /**
     * Check for way node combination.
     * @param primitive the primitive
     */
    private void checkWayNodeCombination(OsmPrimitive primitive) {
        if (primitive instanceof Node) {
            var node = (Node)primitive;
            if (node.get("agv_node_id") != null) {
                var ways = node.getParentWays();
                if (ways.size() != 1)
                    addError(node, 6004, "Node with agv_node_id must be part of 1 way");
                var value = ways.get(0).get("line_info");
                if (value == null || !value.equals("agv_pose"))
                    addError(node, 6004, "Node with agv_node_id must be part of way with line_info=agv_pose");
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
                    if (node0.get("agv_node_id") == null || node1.get("agv_node_id") == null)
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
