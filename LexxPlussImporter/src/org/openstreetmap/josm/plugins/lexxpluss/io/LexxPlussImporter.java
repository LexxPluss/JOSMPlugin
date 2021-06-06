package org.openstreetmap.josm.plugins.lexxpluss.io;

import java.util.Arrays;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.openstreetmap.josm.gui.io.importexport.OsmImporter;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.Logging;

public class LexxPlussImporter extends OsmImporter {

	public static final ExtensionFileFilter FILE_FILTER_LEXX = ExtensionFileFilter.newFilterWithArchiveExtensions(
            "osm,xml", "osm", "LexxPluss format OSM (*.osm, *.xml)",
            ExtensionFileFilter.AddArchiveExtension.NONE, Arrays.asList("gz", "bz", "bz2", "xz", "zip"));

	private File m_importFile;
	private LexxPlussReader m_reader;
	
	public LexxPlussImporter()
	{
		// Use our filter
		super(FILE_FILTER_LEXX);
		m_reader = new LexxPlussReader();
	}
	
	
	@Override
    protected DataSet parseDataSet(InputStream in, ProgressMonitor progressMonitor) throws IllegalDataException
	{
		// replace original OsmReader to our inherited class
        return m_reader.execParse(in, progressMonitor);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException
    {
    	// try to get transform info first
    	try (InputStream in = Compression.getUncompressedFileInputStream(file)) {
    		m_reader.readTransformInfo(in);
    	} catch (FileNotFoundException e) {
    		Logging.error(e);
    		throw new IOException(tr("File ''{0}'' does not exist.", file.getName()), e);
        }
    	// then do normal process
    	super.importData(file, progressMonitor);
    }


}
