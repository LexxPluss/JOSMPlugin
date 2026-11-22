/**
 * Copyright (c) 2026, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

/**
 * Values written to and accepted for boolean-like LexxPluss tags.
 * The written values stay legacy until LexxAuto accepts yes/no (AMRSW-3982).
 */
final class BooleanTagValues {
    // LexxAuto reads these only for oneway until AMRSW-3982.
    static final String YES = "yes";
    static final String NO = "no";

    /** Written by the action. LexxAuto's non-stop area visualizer shows only "true". */
    static final String NON_STOP_AREA_ON = "true";
    /** Written by the action. LexxAuto turns a safety zone off only for "off". */
    static final String SAFETY_OFF = "off";

    // CustomTagTest accepts these because existing maps carry them.
    // Keep accepting them after the written values switch to yes/no.
    static final String LEGACY_NON_STOP_AREA_ON = "true";
    static final String LEGACY_SAFETY_OFF = "off";

    private BooleanTagValues() {
    }
}
