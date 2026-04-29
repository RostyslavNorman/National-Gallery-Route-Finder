package model;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the National Gallery database from {@code gallery_data.xml} into a
 * {@link GalleryGraph} at application startup.
 *
 * <h2>Responsibilities</h2>
 * <ol>
 *   <li>Configure XStream with the correct class and field aliases so that the
 *       XML element names map cleanly to Java class and field names.</li>
 *   <li>Deserialise the XML file into an intermediate {@link GalleryData} holder.</li>
 *   <li>Build a fully connected {@link GalleryGraph} from that data:
 *       add every room as a vertex, then register every connection as a weighted
 *       undirected edge.</li>
 *   <li>Expose the loaded artist list separately so the UI can populate the
 *       artist-preference selector.</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 *   GalleryLoader loader = new GalleryLoader();
 *   GalleryGraph  graph  = loader.getGraph();
 *   List<Artist>  artists = loader.getArtists();
 * }</pre>
 *
 * <h2>File location</h2>
 * <p>{@code gallery_data.xml} must be on the classpath root — place it in
 * {@code src/main/resources/}. Maven copies it to the classpath automatically.</p>
 *
 * <h2>Read-only</h2>
 * <p>The application never writes back to the XML file. To add rooms, connections,
 * or paintings, edit {@code gallery_data.xml} directly and re-run the application.</p>
 */
public class GalleryLoader {

    /** Resource path for the gallery XML database on the classpath. */
    private static final String DATA_FILE = "/gallery_data.xml";

    /** The fully built graph, populated after construction. */
    private final GalleryGraph graph;

    /** The list of all artists loaded from the XML, for use in the UI. */
    private final List<Artist> artists;

    // -------------------------------------------------------------------------
    // Constructor — loads everything on creation
    // -------------------------------------------------------------------------

    /**
     * Creates a new GalleryLoader, immediately reads {@code gallery_data.xml}
     * from the classpath, and builds the graph and artist list.
     *
     * @throws RuntimeException if the file cannot be found or parsed
     */
    public GalleryLoader() {
        GalleryData data = parseXml();
        this.artists = data.artists != null ? data.artists : new ArrayList<>();
        this.graph   = buildGraph(data);
    }

    // -------------------------------------------------------------------------
    // Public accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the fully built {@link GalleryGraph} with all rooms (vertices)
     * and connections (edges) loaded from the XML database.
     *
     * @return the gallery graph; never null
     */
    public GalleryGraph getGraph() {
        return graph;
    }

    /**
     * Returns the list of all artists loaded from the XML database.
     * Used by the UI to populate the artist-preference selection control.
     *
     * @return list of artists; never null, may be empty
     */
    public List<Artist> getArtists() {
        return artists;
    }

    // -------------------------------------------------------------------------
    // Private — XML parsing
    // -------------------------------------------------------------------------

    /**
     * Configures XStream and deserialises {@code gallery_data.xml} into a
     * raw {@link GalleryData} intermediate object.
     *
     * <p>Aliases map XML element names to Java types and fields. This keeps the
     * XML human-readable (short tag names) while keeping Java field names clear.</p>
     *
     * @return the raw deserialised data holder
     * @throws RuntimeException if the resource file is missing or XML is malformed
     */
    private GalleryData parseXml() {
        XStream xstream = new XStream(new StaxDriver());

        // Allow all types — safe here because we only load our own known XML file
        xstream.addPermission(AnyTypePermission.ANY);

        // ── Root and container aliases ────────────────────────────────────────
        xstream.alias("gallery",    GalleryData.class);
        xstream.alias("artist",     Artist.class);
        xstream.alias("room",       RoomData.class);
        xstream.alias("painting",   Painting.class);
        xstream.alias("connection", ConnectionData.class);
        xstream.alias("throughpoint", Point.class);

        // ── Field aliases — map short XML tag names to Java field names ───────

        // GalleryDataParser fields
        xstream.aliasField("artists", GalleryData.class, "artists");
        xstream.aliasField("rooms",   GalleryData.class, "rooms");

        // Artist fields — <n> maps to Artist.name to keep XML concise
        xstream.aliasField("n",           Artist.class, "name");
        xstream.aliasField("nationality", Artist.class, "nationality");
        xstream.aliasField("period",      Artist.class, "period");

        // RoomData fields — <n> maps to name; <id>, <x>, <y> match exactly
        xstream.aliasField("n",           RoomData.class, "name");
        xstream.aliasField("connections", RoomData.class, "connections");
        xstream.aliasField("paintings",   RoomData.class, "paintings");

        // ConnectionData fields
        xstream.aliasField("roomId",   ConnectionData.class, "roomId");
        xstream.aliasField("distance", ConnectionData.class, "distance");
        xstream.aliasField("throughpoint", ConnectionData.class, "throughpoint");

        // Painting fields
        xstream.aliasField("title",         Painting.class, "title");
        xstream.aliasField("artist",        Painting.class, "artist");
        xstream.aliasField("imageFilename", Painting.class, "imageFilename");
        xstream.aliasField("description",   Painting.class, "description");

        // ── Collection mappings ───────────────────────────────────────────────
        // The XML uses explicit wrapper elements (<artists>, <rooms>, <connections>,
        // <paintings>), so we do NOT use implicit collections here. XStream will map
        // the wrapper element directly onto these list fields by name.

        // ── Load the resource from the classpath ──────────────────────────────
        InputStream stream = GalleryLoader.class.getResourceAsStream(DATA_FILE);
        if (stream == null) {
            throw new RuntimeException(
                    "gallery_data.xml not found on classpath. " +
                            "Ensure it is in src/main/resources/ and Maven has run.");
        }

        try (InputStream in = stream) {
            return (GalleryData) xstream.fromXML(in);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse gallery_data.xml", e);
        }
    }

    // -------------------------------------------------------------------------
    // Private — graph construction
    // -------------------------------------------------------------------------

    /**
     * Converts the raw {@link GalleryData} into a connected {@link GalleryGraph}.
     *
     * <p>Pass 1 — add all rooms as vertices (so every room exists before any
     * edge is registered).</p>
     * <p>Pass 2 — iterate each room's connections and call
     * {@link GalleryGraph connectRooms(String, String, double)} for each one.
     * Because {@code connectRooms} stores the edge in both directions, and the
     * XML defines each connection from one side only, we get a correct undirected
     * graph without duplicate edges.</p>
     *
     * @param data the raw deserialised data
     * @return a fully built GalleryGraph
     */
    private GalleryGraph buildGraph(GalleryData data) {
        GalleryGraph g = new GalleryGraph();

        if (data.rooms == null || data.rooms.isEmpty()) {
            System.err.println("WARNING: gallery_data.xml contains no rooms.");
            return g;
        }

        // Pass 1 — add all rooms as vertices first
        for (RoomData rd : data.rooms) {
            Room room = new Room(rd.id, rd.name, rd.x, rd.y);

            if (rd.paintings != null) {
                for (Painting p : rd.paintings) {
                    room.addPainting(p);
                }
            }

            g.addRoom(room);
        }

        // Pass 2 — register all edges
        for (RoomData rd : data.rooms) {
            if (rd.connections == null) continue;

            for (ConnectionData conn : rd.connections) {

                // Guard: skip if target room doesn't exist in the graph
                if (!g.containsRoom(conn.roomId)) {
                    System.err.println("WARNING: Room " + rd.id +
                            " references unknown neighbour " + conn.roomId +
                            " — skipping this connection.");
                    continue;
                }

                // Guard: skip self-loops
                if (rd.id.equals(conn.roomId)) {
                    System.err.println("WARNING: Room " + rd.id +
                            " has a self-loop connection — skipping.");
                    continue;
                }

                // Guard: skip if edge already exists (avoid duplicates)
                if (g.areConnected(rd.id, conn.roomId)) continue;

                // Pull throughpoints from the connection, default to empty list
                // Replace the manual single-point wrapping with:
                List<Point> throughpoints = conn.throughpoints != null
                        ? conn.throughpoints
                        : new ArrayList<>();
                g.connectRooms(rd.id, conn.roomId, conn.distance, throughpoints);
            }
        }

        System.out.println("GalleryLoader: loaded " + g.getRoomCount() +
                " rooms and " + g.getEdgeCount() + " connections.");

        return g;
    }

    // =========================================================================
    // Private inner data-holder classes (XStream intermediate objects)
    // These are NOT part of the model — they are only used during loading.
    // =========================================================================

    /**
     * Top-level data holder, mapped from the {@code <gallery>} XML root element.
     * Holds the raw lists of artists and rooms before graph construction.
     */
    private static class GalleryData {
        List<Artist>   artists;
        List<RoomData> rooms;
    }

    /**
     * Intermediate room holder, mapped from each {@code <room>} element.
     * Contains raw field values and unparsed connection/painting lists.
     * Converted to a proper {@link Room} during graph construction.
     */
    private static class RoomData {
        String               id;
        String               name;
        int                  x;
        int                  y;
        List<ConnectionData> connections;
        List<Painting>       paintings;
    }

    /**
     * Intermediate connection holder, mapped from each {@code <connection>} element.
     * Converted to a {@link GalleryGraph.Edge} during graph construction.
     */
    private static class ConnectionData {
        String roomId;
        double distance;
        List<Point> throughpoints;
    }
}

