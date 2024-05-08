/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class IdDuplicateTest extends Test {

    /**
     * Constructs a new {@code IdDuplicateTest}.
     */
    public IdDuplicateTest() {
        super("Duplicate ID check (LexxPluss)", "Duplicate ID check for LexxPluss");
    }

    @Override
    public void visit(Node node) {
        var other_nodes = node.getDataSet().getNodes().stream()
                .filter(n -> n != node)
                .collect(Collectors.toList());
        Arrays.asList("agv_node_id", "intermediate_goal_id")
                .forEach(key -> checkDuplicateTagValue(node, key, other_nodes));
    }

    @Override
    public void visit(Way way) {
        var other_ways = way.getDataSet().getWays().stream()
                .filter(w -> w != way)
                .collect(Collectors.toList());
        Arrays.asList("goal_id", "space_id", "sync_id", "area_name")
                .forEach(key -> checkDuplicateTagValue(way, key, other_ways));
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
            if (values.contains(value)) {
                errors.add(TestError.builder(this, Severity.ERROR, 6001)
                        .message("Duplicate tag key:" + key + " value:" + value)
                        .primitives(current)
                        .build());
            }
        }
    }
}
