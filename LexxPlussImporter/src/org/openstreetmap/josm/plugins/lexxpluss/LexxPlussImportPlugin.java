// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.lexxpluss;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.lexxpluss.io.LexxPlussImporter;

/**
 * 
 * LexxPluss Plugin
 * @author LexxPluss
 *
 */
public class LexxPlussImportPlugin extends Plugin {

    public LexxPlussImportPlugin(PluginInformation info) {
        super(info);
        ExtensionFileFilter.addImporter(new LexxPlussImporter());
    }
}
