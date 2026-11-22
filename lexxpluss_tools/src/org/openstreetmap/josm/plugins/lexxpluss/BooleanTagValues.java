/**
 * Copyright (c) 2026, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

/**
 * Values written to and accepted for boolean-like LexxPluss tags.
 * IOP-1821 moves the legacy values to yes/no after LexxAuto accepts yes/no.
 */
final class BooleanTagValues {
    static final String YES = "yes";
    static final String NO = "no";

    /** Legacy value. LexxAuto's non-stop area visualizer shows only "true". */
    static final String NON_STOP_AREA_ON = "true";
    /** Legacy value. LexxAuto turns a safety zone off only for "off". */
    static final String SAFETY_OFF = "off";

    private BooleanTagValues() {
    }
}
