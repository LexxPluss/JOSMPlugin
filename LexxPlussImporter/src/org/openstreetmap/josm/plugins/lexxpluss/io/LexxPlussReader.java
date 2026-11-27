package org.openstreetmap.josm.plugins.lexxpluss.io;

import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.TagMap;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.TreeSet;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.XmlUtils;
import static org.openstreetmap.josm.data.projection.Ellipsoid.WGS84;




public class LexxPlussReader extends OsmReader {

	// for converting to lat long
	private AffineTransform m_transform;
	private double m_view_center_lon = 0.0;
	private double m_view_center_lat = 0.0;
	private double m_pixel_per_en_x = -1.0;
	private double m_pixel_per_en_y = -1.0;
	private double m_pic_offset_x = 0.0;
	private double m_pic_offset_y = 0.0;
	private double m_scaleX = 0.0;
	private double m_scaleY = 0.0;
	private double m_hh = 0.0;
	private double m_hw = 0.0;

	public boolean readTransformInfo(InputStream source)
	{
		// LexxPlussImporter reuses one reader for every import. Without this, a file without a usable
		// matrix way would be converted with the previous file's transform.
		this.m_transform = null;
		// Same choice as LexxPlussExporter: a live way without nodes, then the highest id
		// (docs/adr/0002-readers-pick-the-exported-matrix-way.md). JOSM writes and renumbers new ways
		// in id order, so the highest id in the file is the highest unique id the exporter compares.
		TagMap best = null;
		long bestId = Long.MIN_VALUE;
		boolean bestHasNodes = false;
		try (InputStreamReader ir = UTFInputStreamReader.create(source)) {
			XMLStreamReader local_parser = XmlUtils.newSafeXMLInputFactory().createXMLStreamReader(ir);
			while (local_parser.hasNext()) {
				if (local_parser.next() != XMLStreamConstants.START_ELEMENT || !"way".equals(local_parser.getLocalName())) {
					continue;
				}
				long id = Long.parseLong(local_parser.getAttributeValue(null, "id"));
				boolean deleted = "delete".equals(local_parser.getAttributeValue(null, "action"));
				TagMap tags = new TagMap();
				boolean hasNodes = false;
				int depth = 1;
				while (depth > 0 && local_parser.hasNext()) {
					int event = local_parser.next();
					if (event == XMLStreamConstants.START_ELEMENT) {
						depth++;
						if ("nd".equals(local_parser.getLocalName())) {
							hasNodes = true;
						} else if ("tag".equals(local_parser.getLocalName())) {
							tags.put(local_parser.getAttributeValue(null, "k"), local_parser.getAttributeValue(null, "v"));
						}
					} else if (event == XMLStreamConstants.END_ELEMENT) {
						depth--;
					}
				}
				if (deleted || !tags.containsKey("transform matrix")) {
					continue;
				}
				if (best == null || (hasNodes != bestHasNodes ? !hasNodes : id > bestId)) {
					best = tags;
					bestId = id;
					bestHasNodes = hasNodes;
				}
			}
			local_parser.close();
		}
		catch(Exception e) {
			return false;
		}
		if (best == null) {
			return false;
		}
		this.setTransformInfo(best);
		return true;
	}

	private void setTransformInfo(TagMap tags) {
		double[] matrix = new double[6];
		try {
			matrix[0] = Double.parseDouble(tags.get("m0"));
			matrix[1] = Double.parseDouble(tags.get("m1"));
			matrix[2] = Double.parseDouble(tags.get("m2"));
			matrix[3] = Double.parseDouble(tags.get("m3"));
			matrix[4] = Double.parseDouble(tags.get("m4"));
			matrix[5] = Double.parseDouble(tags.get("m5"));
			this.m_view_center_lon = Double.parseDouble(tags.get("view_center_lon"));
			this.m_view_center_lat = Double.parseDouble(tags.get("view_center_lat"));
			this.m_pixel_per_en_x = Double.parseDouble(tags.get("pixel_per_en_x"));
			this.m_pixel_per_en_y = Double.parseDouble(tags.get("pixel_per_en_y"));
			this.m_pic_offset_x = Double.parseDouble(tags.get("pic_offset_x"));
			this.m_pic_offset_y = Double.parseDouble(tags.get("pic_offset_y"));
			this.m_scaleX = Double.parseDouble(tags.get("scaleX"));
			this.m_scaleY = Double.parseDouble(tags.get("scaleY"));
			this.m_hw = Double.parseDouble(tags.get("hw"));
			this.m_hh = Double.parseDouble(tags.get("hh"));
		}
		catch(Exception e) {
			System.out.println("Failed to get transform matrix");
		}
		this.m_transform = new AffineTransform(matrix);
	}

	public DataSet execParse(InputStream source, ProgressMonitor progressMonitor)
			throws IllegalDataException
	{
		return doParseDataSet(source, progressMonitor);
	}
	
	// Convert X_image, Y_image (pixel) to Longitude, Latitude
	protected void convert2LatLon(NodeData nd)
	{
		int num = nd.getNumKeys();
		TagMap mk = nd.getKeys();
		if ((mk.containsKey("X_image")) && (mk.containsKey("Y_image"))) {
			try {
				double x = Double.parseDouble(mk.get("X_image"));
				double y = Double.parseDouble(mk.get("Y_image"));
				x -= this.m_hw;
				y -= this.m_hh;
				x /= this.m_scaleX;
				y /= this.m_scaleY;
				x += this.m_pic_offset_x / m_transform.getScaleX();
				y += this.m_pic_offset_y / m_transform.getScaleY();
				double[] in_point = {x, y};
				double[] cvt_point = new double[2];
				m_transform.transform(in_point, 0, cvt_point, 0, 1);
				double east = (cvt_point[0] / this.m_pixel_per_en_x) + this.m_view_center_lon;
				double north = this.m_view_center_lat - (cvt_point[1] / this.m_pixel_per_en_y);
				EastNorth pos = new EastNorth(east, north);
				nd.setEastNorth(pos);
			}
			catch (Exception e) {
				return;
			}
			return;
		}
	}

	@Override
	protected Node parseNode() throws XMLStreamException {
		String lat = parser.getAttributeValue(null, "lat");
		String lon = parser.getAttributeValue(null, "lon");
		if (lat == null || lon == null || (0 >= lat.length()) || (0 >= lon.length())) {
			NodeData nd = new NodeData(0);
			try {
				readCommon(nd);
				parseNodeTags(nd);
				convert2LatLon(nd);
				return (Node) buildPrimitive(nd);
			}catch (Exception e) {
				
			}
			
		}
		try {
			return parseNode(lat, lon, this::readCommon, this::parseNodeTags);
		} catch (IllegalDataException e) {
			handleIllegalDataException(e);
		}
		return null;
	}

	/*
	 * Below From here, redefine super class functions due to private classes
	 * but, these are completely same as super
	 * ここから下は、継承元のクラスの関数がprivateでアクセスできなかったために、
	 * 関数をそのままコピーしているだけで、特に変更は加えていない。
	 * 継承元関数: https://github.com/openstreetmap/josm/blob/597cab135f3dad3569b1c273162910a0af88bb8e/src/org/openstreetmap/josm/io/OsmReader.java
	 * */
	private static final Set<String> COMMON_XML_ATTRIBUTES = new TreeSet<>();

	static {
		COMMON_XML_ATTRIBUTES.add("id");
		COMMON_XML_ATTRIBUTES.add("timestamp");
		COMMON_XML_ATTRIBUTES.add("user");
		COMMON_XML_ATTRIBUTES.add("uid");
		COMMON_XML_ATTRIBUTES.add("visible");
		COMMON_XML_ATTRIBUTES.add("version");
		COMMON_XML_ATTRIBUTES.add("action");
		COMMON_XML_ATTRIBUTES.add("changeset");
		COMMON_XML_ATTRIBUTES.add("lat");
		COMMON_XML_ATTRIBUTES.add("lon");
	}

	protected void handleIllegalDataException(IllegalDataException e) throws XMLStreamException {
		Throwable cause = e.getCause();
		if (cause instanceof XMLStreamException) {
			throw (XMLStreamException) cause;
		} else {
			throwException(e);
		}
	}

	private void parseNodeTags(NodeData n) throws IllegalDataException {
		try {
			while (parser.hasNext()) {
				int event = parser.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					if ("tag".equals(parser.getLocalName())) {
						parseTag(n);
					} else {
						parseUnknown();
					}
				} else if (event == XMLStreamConstants.END_ELEMENT) {
					return;
				}
			}
		} catch (XMLStreamException e) {
			throw new IllegalDataException(e);
		}
	}

	private void parseTag(Tagged t) throws XMLStreamException {
		String key = parser.getAttributeValue(null, "k");
		String value = parser.getAttributeValue(null, "v");
		try {
			parseTag(t, key, value);
		} catch (IllegalDataException e) {
			throwException(e);
		}
		jumpToEnd();
	}

	private void readCommon(PrimitiveData current) throws IllegalDataException {
		try {
			parseId(current, getLong("id"));
			parseTimestamp(current, parser.getAttributeValue(null, "timestamp"));
			parseUser(current, parser.getAttributeValue(null, "user"), parser.getAttributeValue(null, "uid"));
			parseVisible(current, parser.getAttributeValue(null, "visible"));
			parseVersion(current, parser.getAttributeValue(null, "version"));
			parseAction(current, parser.getAttributeValue(null, "action"));
			parseChangeset(current, parser.getAttributeValue(null, "changeset"));

			if (options.contains(Options.SAVE_ORIGINAL_ID)) {
				parseTag(current, "current_id", Long.toString(getLong("id")));
			}
			if (options.contains(Options.CONVERT_UNKNOWN_TO_TAGS)) {
				for (int i = 0; i < parser.getAttributeCount(); i++) {
					if (!COMMON_XML_ATTRIBUTES.contains(parser.getAttributeLocalName(i))) {
						parseTag(current, parser.getAttributeLocalName(i), parser.getAttributeValue(i));
					}
				}
			}
		} catch (UncheckedParseException | XMLStreamException e) {
			throw new IllegalDataException(e);
		}
	}

	private long getLong(String name) throws XMLStreamException {
		String value = parser.getAttributeValue(null, name);
		try {
			return getLong(name, value);
		} catch (IllegalDataException e) {
			throwException(e);
		}
		return 0; // should not happen
	}
}
