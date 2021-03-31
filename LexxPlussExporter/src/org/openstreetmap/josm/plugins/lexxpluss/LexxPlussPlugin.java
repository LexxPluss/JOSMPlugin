// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.lexxpluss;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.lexxpluss.io.LexxPlussExporter;

/**
 * 
 * LexxPluss Plugin
 * @author LexxPluss
 *
 */
public class LexxPlussPlugin extends Plugin {

    public LexxPlussPlugin(PluginInformation info) {
        super(info);
        ExtensionFileFilter.addExporterFirst(new LexxPlussExporter());
    }
}
