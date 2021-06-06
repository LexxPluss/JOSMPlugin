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
		boolean exist = false;
		try (InputStreamReader ir = UTFInputStreamReader.create(source)) {
			XMLStreamReader local_parser = XmlUtils.newSafeXMLInputFactory().createXMLStreamReader(ir);
			System.out.println("");
			// parse OSM
			if (XMLStreamConstants.START_ELEMENT == local_parser.next() && "osm".equals(local_parser.getLocalName())) {
				while(local_parser.hasNext()) {
					int event = local_parser.next();
					if (event == XMLStreamConstants.START_ELEMENT) {
						switch (local_parser.getLocalName()) {
						case "way":
							{
								TagMap tags = new TagMap();
								while (local_parser.hasNext()) {
									int event_w = local_parser.next();
									if (event_w == XMLStreamConstants.START_ELEMENT) {
										if ("tag".equals(local_parser.getLocalName())) {
											String key = local_parser.getAttributeValue(null, "k");
											String value = local_parser.getAttributeValue(null, "v");
											tags.put(key, value);
											if ("transform matrix".equals(key)) {
												exist = true;
											}
											// jump to end
											while(XMLStreamConstants.END_ELEMENT == local_parser.next());
										}
									}
									else if (event_w == XMLStreamConstants.END_ELEMENT) {
									    break;
									}
								}
								if (exist) {
									// set matrix here
									this.setTransformInfo(tags);
								}
							}
							break;
						
						default:
							{
								// jump to end
								while(XMLStreamConstants.END_ELEMENT == local_parser.next());							
							}
							break;
						}
						if (exist) {
							break;
						}
					}
				}
			}
			local_parser.close();
		}
		catch(Exception e) {
			System.out.println("");
			return false;
		}
		return exist;
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
        if ((0 >= lat.length()) || (0 >= lon.length())) {
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
	 * above From here, redefine super class functions due to private classes
	 * but, these are completely same as super
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
