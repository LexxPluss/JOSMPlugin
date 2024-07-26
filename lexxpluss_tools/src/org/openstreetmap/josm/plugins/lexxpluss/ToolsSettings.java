/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Settings for the LexxPluss Tools plugin.
 */
public class ToolsSettings {

    /**
     * Prefix for the configuration keys.
     */
    private static String prefix = "lexxpluss_tools.";

    /**
     * Private constructor to avoid instantiation.
     */
    private ToolsSettings() {
    }

    /**
     * Get the host name.
     * @return the host name
     */
    public static String getHost() {
        return Config.getPref().get(prefix + "host", "192.168.1.10");
    }

    /**
     * Set the hostname.
     * @param host the hostname
     */
    public static void setHost(String host) {
        Config.getPref().put(prefix + "host", host);
    }

    /**
     * Get the user name.
     * @return the user name
     */
    public static String getUser() {
        return Config.getPref().get(prefix + "user", "lexxpluss");
    }

    /**
     * Set the user name.
     * @param user the user name
     */
    public static void setUser(String user) {
        Config.getPref().put(prefix + "user", user);
    }

    /**
     * Get the password.
     * @return the password
     */
    public static String getPassword() {
        return Config.getPref().get(prefix + "password", "");
    }

    /**
     * Set the password.
     * @param password the password
     */
    public static void setPassword(String password) {
        Config.getPref().put(prefix + "password", password);
    }

    /**
     * Get the use identity flag.
     * @return the use identity flag
     */
    public static boolean getUseIdentity() {
        return Config.getPref().getBoolean(prefix + "useIdentity", false);
    }

    /**
     * Set the use identity flag.
     * @param useIdentity the use identity flag
     */
    public static void setUseIdentity(boolean useIdentity) {
        Config.getPref().putBoolean(prefix + "useIdentity", useIdentity);
    }

    /**
     * Get the identity path.
     * @return the identity path
     */
    public static String getIdentityPath() {
        return Config.getPref().get(prefix + "identityPath", "");
    }

    /**
     * Set the identity path.
     * @param identityPath the identity path
     */
    public static void setIdentityPath(String identityPath) {
        Config.getPref().put(prefix + "identityPath", identityPath);
    }

    /**
     * Get the OSM path.
     * @return the OSM path
     */
    public static String getOsmPath() {
        return Config.getPref().get(prefix + "osmPath", "/home/lexxpluss/osm/map.osm");
    }

    /**
     * Set the OSM path.
     * @param osmPath the OSM path
     */
    public static void setOsmPath(String osmPath) {
        Config.getPref().put(prefix + "osmPath", osmPath);
    }
}
