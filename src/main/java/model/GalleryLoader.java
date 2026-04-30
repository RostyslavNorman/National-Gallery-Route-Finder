package model;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GalleryLoader {

    private static final String DATA_FILE = "/gallery_data.xml";

    private final GalleryGraph graph;
    private final List<Artist> artists;

    public GalleryLoader() {
        GalleryData data = parseXml();
        this.artists = data.artists != null ? data.artists : new ArrayList<>();
        this.graph   = buildGraph(data);
    }

    public GalleryGraph getGraph()    { return graph; }
    public List<Artist> getArtists() { return artists; }

    private GalleryData parseXml() {
        XStream xstream = new XStream(new StaxDriver());
        xstream.addPermission(AnyTypePermission.ANY);
        xstream.registerConverter(new ConnectionDataConverter());

        xstream.alias("gallery",      GalleryData.class);
        xstream.alias("artist",       Artist.class);
        xstream.alias("room",         RoomData.class);
        xstream.alias("painting",     Painting.class);
        xstream.alias("connection",   ConnectionData.class);
        xstream.alias("throughpoint", Point.class);

        xstream.aliasField("n",           Artist.class,  "name");
        xstream.aliasField("nationality", Artist.class,  "nationality");
        xstream.aliasField("period",      Artist.class,  "period");
        xstream.aliasField("n",           RoomData.class, "name");
        xstream.aliasField("connections", RoomData.class, "connections");
        xstream.aliasField("paintings",   RoomData.class, "paintings");
        xstream.aliasField("roomId",      ConnectionData.class, "roomId");
        xstream.aliasField("distance",    ConnectionData.class, "distance");
        xstream.aliasField("title",       Painting.class, "title");
        xstream.aliasField("artist",      Painting.class, "artist");
        xstream.aliasField("imageFilename", Painting.class, "imageFilename");
        xstream.aliasField("description", Painting.class, "description");

        InputStream stream = GalleryLoader.class.getResourceAsStream(DATA_FILE);
        if (stream == null) throw new RuntimeException("gallery_data.xml not found on classpath.");

        try (InputStream in = stream) {
            return (GalleryData) xstream.fromXML(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse gallery_data.xml", e);
        }
    }

    private GalleryGraph buildGraph(GalleryData data) {
        GalleryGraph g = new GalleryGraph();

        if (data.rooms == null || data.rooms.isEmpty()) {
            System.err.println("WARNING: gallery_data.xml contains no rooms.");
            return g;
        }

        // Build a quick lookup map: id -> RoomData (needed for coordinate access)
        java.util.Map<String, RoomData> roomDataMap = new java.util.HashMap<>();
        for (RoomData rd : data.rooms) {
            roomDataMap.put(rd.id, rd);
        }

        // Pass 1: add all rooms as vertices
        for (RoomData rd : data.rooms) {
            Room room = new Room(rd.id, rd.name, rd.x, rd.y);
            if (rd.paintings != null) {
                for (Painting p : rd.paintings) room.addPainting(p);
            }
            g.addRoom(room);
        }

        // Pass 2: register edges, calculating distance from coordinates automatically
        for (RoomData rd : data.rooms) {
            if (rd.connections == null) continue;

            for (ConnectionData conn : rd.connections) {
                if (!g.containsRoom(conn.roomId)) {
                    System.err.println("WARNING: Room " + rd.id +
                            " references unknown neighbour " + conn.roomId + " — skipping.");
                    continue;
                }
                if (rd.id.equals(conn.roomId)) {
                    System.err.println("WARNING: Room " + rd.id + " has a self-loop — skipping.");
                    continue;
                }
                if (g.areConnected(rd.id, conn.roomId)) continue;

                // Auto-calculate distance from pixel coordinates if not set in XML (<=0)
                double distance = conn.distance;
                if (distance <= 0) {
                    RoomData target = roomDataMap.get(conn.roomId);
                    if (target != null) {
                        double dx = rd.x - target.x;
                        double dy = rd.y - target.y;
                        distance = Math.sqrt(dx * dx + dy * dy);
                    }
                    if (distance <= 0) distance = 1; // fallback
                }

                List<Point> throughpoints = conn.throughpoints != null
                        ? conn.throughpoints : new ArrayList<>();
                g.connectRooms(rd.id, conn.roomId, distance, throughpoints);
            }
        }

        System.out.println("GalleryLoader: loaded " + g.getRoomCount() +
                " rooms and " + g.getEdgeCount() + " connections.");
        return g;
    }

    // Custom converter — handles optional <throughpoint> children inside <connection>
    private static class ConnectionDataConverter
            implements com.thoughtworks.xstream.converters.Converter {

        @Override
        public boolean canConvert(Class type) { return type == ConnectionData.class; }

        @Override
        public void marshal(Object source,
                            com.thoughtworks.xstream.io.HierarchicalStreamWriter writer,
                            com.thoughtworks.xstream.converters.MarshallingContext context) {}

        @Override
        public Object unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader reader,
                                com.thoughtworks.xstream.converters.UnmarshallingContext context) {
            ConnectionData conn = new ConnectionData();
            conn.throughpoints = new ArrayList<>();

            while (reader.hasMoreChildren()) {
                reader.moveDown();
                switch (reader.getNodeName()) {
                    case "roomId"   -> conn.roomId = reader.getValue();
                    case "distance" -> conn.distance = Double.parseDouble(reader.getValue());
                    case "throughpoint" -> {
                        Point p = new Point();
                        while (reader.hasMoreChildren()) {
                            reader.moveDown();
                            if ("x".equals(reader.getNodeName()))
                                p.x = Integer.parseInt(reader.getValue());
                            else if ("y".equals(reader.getNodeName()))
                                p.y = Integer.parseInt(reader.getValue());
                            reader.moveUp();
                        }
                        conn.throughpoints.add(p);
                    }
                }
                reader.moveUp();
            }
            return conn;
        }
    }

    private static class GalleryData {
        List<Artist>   artists;
        List<RoomData> rooms;
    }

    private static class RoomData {
        String               id;
        String               name;
        int                  x, y;
        List<ConnectionData> connections;
        List<Painting>       paintings;
    }

    private static class ConnectionData {
        String     roomId;
        double     distance;
        List<Point> throughpoints;
    }
}