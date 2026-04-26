package algorithms;

import model.GalleryGraph;
import model.GalleryGraph.Edge;
import model.Artist;
import model.Painting;
import model.Room;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * All route-finding algorithms for the National Gallery Route Finder.
 *
 * Everything is static — no state is stored here. The graph and search
 * parameters are passed in on each call, so each method is easy to test
 * in isolation.
 *
 * Algorithms:
 *   findSingleRoute           - DFS, returns one valid route
 *   findMultipleRoutes        - DFS, returns up to N routes
 *   findShortestRouteBFS      - BFS, fewest rooms (unweighted)
 *   findShortestRouteDijkstra - Dijkstra, minimum walking distance
 *   findMostInterestingRoute  - Dijkstra biased toward preferred artists
 *   findPixelRoute            - BFS on a map image pixel by pixel
 *
 * Waypoints: all graph methods accept an ordered waypoints list.
 * The route is split into segments (start -> wp1 -> wp2 -> end) and
 * the chosen algorithm runs on each segment. Segments are joined with
 * duplicate junction rooms removed.
 *
 * Avoid rooms: all graph methods accept an avoidRooms set. Rooms in
 * that set are skipped entirely during search.
 */
public class SearchAlgorithms {

    // How much an edge weight is reduced per painting by a preferred artist.
    // Higher = algorithm steers toward those rooms more aggressively.
    private static final double INTEREST_BONUS = 30.0;

    // -------------------------------------------------------------------------
    // 1. DFS — Single Route
    // -------------------------------------------------------------------------

    /**
     * Finds any single valid route between two rooms using DFS.
     *
     * Stops as soon as the destination is reached, so this won't necessarily
     * be the shortest path — just the first one DFS stumbles upon.
     *
     * @param graph      the gallery graph
     * @param startId    starting room ID
     * @param endId      destination room ID
     * @param waypoints  rooms to pass through in order; empty list for direct route
     * @param avoidRooms rooms to treat as impassable
     * @return list of rooms from start to end; empty if no route exists
     */
    public static List<Room> findSingleRoute(GalleryGraph graph,
                                             String startId,
                                             String endId,
                                             List<String> waypoints,
                                             Set<String> avoidRooms) {
        List<String> stops = buildStops(startId, endId, waypoints);

        return chainSegments(graph, stops, avoidRooms, (g, a, b, avoid) -> {
            List<Room> result = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            dfsSingle(g, a, b, visited, result, avoid);
            return result;
        });
    }

    /**
     * Recursive DFS helper. Explores depth-first and backtracks on dead ends.
     *
     * @param graph      the gallery graph
     * @param currentId  room currently being visited
     * @param endId      destination room ID
     * @param visited    rooms already on the current path (prevents cycles)
     * @param path       rooms on the current path, mutated in place
     * @param avoidRooms rooms to skip
     * @return true if destination was found and path is complete
     */
    private static boolean dfsSingle(GalleryGraph graph,
                                     String currentId,
                                     String endId,
                                     Set<String> visited,
                                     List<Room> path,
                                     Set<String> avoidRooms) {
        visited.add(currentId);
        path.add(graph.getRoom(currentId));

        if (currentId.equals(endId)) return true;

        for (Edge edge : graph.getNeighbours(currentId)) {
            String neighbourId = edge.getTargetRoomId();
            if (!visited.contains(neighbourId) && !avoidRooms.contains(neighbourId)) {
                if (dfsSingle(graph, neighbourId, endId, visited, path, avoidRooms))
                    return true; // found it — no need to keep searching
            }
        }

        // Dead end — backtrack
        path.remove(path.size() - 1);
        return false;
    }

    // -------------------------------------------------------------------------
    // 2. DFS — Multiple Routes
    // -------------------------------------------------------------------------

    /**
     * Finds multiple routes between two rooms using DFS.
     *
     * Keeps going until all paths are exhausted or maxRoutes is hit.
     * The number of permutations can blow up fast in dense graphs,
     * so always pass a sensible limit (e.g. 10).
     *
     * @param graph      the gallery graph
     * @param startId    starting room ID
     * @param endId      destination room ID
     * @param waypoints  rooms to pass through in order
     * @param avoidRooms rooms to treat as impassable
     * @param maxRoutes  maximum number of routes to return; must be >= 1
     * @return list of routes, each route being a list of rooms
     */
    public static List<List<Room>> findMultipleRoutes(GalleryGraph graph,
                                                      String startId,
                                                      String endId,
                                                      List<String> waypoints,
                                                      Set<String> avoidRooms,
                                                      int maxRoutes) {
        if (maxRoutes < 1) throw new IllegalArgumentException("maxRoutes must be at least 1.");

        List<String> stops = buildStops(startId, endId, waypoints);
        List<List<Room>> allRoutes = new ArrayList<>();

        collectMultipleRoutes(graph, stops, avoidRooms, maxRoutes, new ArrayList<>(), allRoutes);
        return allRoutes;
    }

    /**
     * Recursively builds complete routes by collecting all segment permutations
     * at each waypoint gap and combining them.
     *
     * @param graph       the gallery graph
     * @param stops       ordered room IDs to pass through
     * @param avoidRooms  rooms to skip
     * @param maxRoutes   stop once this many full routes have been collected
     * @param currentPath rooms collected so far (partial path)
     * @param allRoutes   accumulator for finished routes
     */
    private static void collectMultipleRoutes(GalleryGraph graph,
                                              List<String> stops,
                                              Set<String> avoidRooms,
                                              int maxRoutes,
                                              List<Room> currentPath,
                                              List<List<Room>> allRoutes) {
        // All stops consumed — save the completed path
        if (stops.size() == 1) {
            if (currentPath.isEmpty()) return;
            allRoutes.add(new ArrayList<>(currentPath));
            return;
        }

        String fromId = stops.get(0);
        String toId   = stops.get(1);

        // Find all DFS paths between this pair of stops
        List<List<Room>> segments = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        List<Room> segment = new ArrayList<>();
        dfsMultiple(graph, fromId, toId, visited, segment, avoidRooms, segments, maxRoutes);

        List<String> remainingStops = stops.subList(1, stops.size());
        for (List<Room> seg : segments) {
            if (allRoutes.size() >= maxRoutes) return;

            // Append segment to current path, removing the duplicate junction room
            List<Room> extended = new ArrayList<>(currentPath);
            if (!extended.isEmpty() && !seg.isEmpty()
                    && extended.get(extended.size() - 1).getId().equals(seg.get(0).getId())) {
                extended.addAll(seg.subList(1, seg.size()));
            } else {
                extended.addAll(seg);
            }

            collectMultipleRoutes(graph, remainingStops, avoidRooms, maxRoutes, extended, allRoutes);
        }
    }

    /**
     * Recursive DFS helper that collects ALL paths to endId, not just the first.
     * Unlike dfsSingle, this keeps exploring after finding a solution.
     *
     * @param graph      the gallery graph
     * @param currentId  room currently being explored
     * @param endId      destination room ID
     * @param visited    rooms on the current path (prevents cycles)
     * @param path       rooms on the current path
     * @param avoidRooms rooms to skip
     * @param results    accumulator for complete paths
     * @param maxRoutes  stop once this many segment paths are collected
     */
    private static void dfsMultiple(GalleryGraph graph,
                                    String currentId,
                                    String endId,
                                    Set<String> visited,
                                    List<Room> path,
                                    Set<String> avoidRooms,
                                    List<List<Room>> results,
                                    int maxRoutes) {
        if (results.size() >= maxRoutes) return;

        visited.add(currentId);
        path.add(graph.getRoom(currentId));

        if (currentId.equals(endId)) {
            results.add(new ArrayList<>(path)); // snapshot the current path
        } else {
            for (Edge edge : graph.getNeighbours(currentId)) {
                String neighbourId = edge.getTargetRoomId();
                if (!visited.contains(neighbourId) && !avoidRooms.contains(neighbourId)) {
                    dfsMultiple(graph, neighbourId, endId, visited, path,
                            avoidRooms, results, maxRoutes);
                    if (results.size() >= maxRoutes) break;
                }
            }
        }

        // Backtrack
        path.remove(path.size() - 1);
        visited.remove(currentId);
    }

    // -------------------------------------------------------------------------
    // 3. BFS — Shortest Route (fewest rooms)
    // -------------------------------------------------------------------------

    /**
     * Finds the shortest route by number of rooms visited using BFS.
     *
     * BFS guarantees the fewest-hops path because it explores all rooms
     * at distance N before touching anything at distance N+1. This is the
     * room-graph BFS — see findPixelRoute for the image-based version.
     *
     * @param graph      the gallery graph
     * @param startId    starting room ID
     * @param endId      destination room ID
     * @param waypoints  rooms to pass through in order
     * @param avoidRooms rooms to treat as impassable
     * @return shortest route as a list of rooms; empty if no route exists
     */
    public static List<Room> findShortestRouteBFS(GalleryGraph graph,
                                                  String startId,
                                                  String endId,
                                                  List<String> waypoints,
                                                  Set<String> avoidRooms) {
        List<String> stops = buildStops(startId, endId, waypoints);
        return chainSegments(graph, stops, avoidRooms, SearchAlgorithms::bfsSegment);
    }

    /**
     * BFS between two rooms. Tracks where each room was discovered from
     * so the path can be reconstructed once we reach the destination.
     *
     * @param graph      the gallery graph
     * @param startId    starting room ID
     * @param endId      destination room ID
     * @param avoidRooms rooms to skip
     * @return shortest hop-count path; empty if unreachable
     */
    private static List<Room> bfsSegment(GalleryGraph graph,
                                         String startId,
                                         String endId,
                                         Set<String> avoidRooms) {
        // parent[roomId] = the room we came from; null means this is the start
        Map<String, String> parent = new HashMap<>();
        Queue<String> queue = new LinkedList<>();

        parent.put(startId, null);
        queue.add(startId);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (current.equals(endId)) return reconstructPath(graph, parent, endId);

            for (Edge edge : graph.getNeighbours(current)) {
                String neighbourId = edge.getTargetRoomId();
                if (!parent.containsKey(neighbourId) && !avoidRooms.contains(neighbourId)) {
                    parent.put(neighbourId, current);
                    queue.add(neighbourId);
                }
            }
        }

        return Collections.emptyList(); // unreachable
    }

    // -------------------------------------------------------------------------
    // 4. Dijkstra's — Shortest Route (minimum distance)
    // -------------------------------------------------------------------------

    /**
     * Finds the shortest route by total walking distance using Dijkstra's algorithm.
     *
     * Uses a min-heap ordered by cumulative distance. The first time the
     * destination is popped from the queue it's guaranteed to be via the
     * cheapest path, since all edge weights are positive.
     *
     * @param graph      the gallery graph
     * @param startId    starting room ID
     * @param endId      destination room ID
     * @param waypoints  rooms to pass through in order
     * @param avoidRooms rooms to treat as impassable
     * @return shortest route as a list of rooms; empty if no route exists
     */
    public static List<Room> findShortestRouteDijkstra(GalleryGraph graph,
                                                       String startId,
                                                       String endId,
                                                       List<String> waypoints,
                                                       Set<String> avoidRooms) {
        List<String> stops = buildStops(startId, endId, waypoints);
        return chainSegments(graph, stops, avoidRooms,
                (g, a, b, avoid) -> dijkstraSegment(g, a, b, avoid, null));
    }

    /**
     * Core Dijkstra implementation for one segment of the route.
     *
     * When preferredArtists is non-null, the effective edge weight into a room
     * is reduced by INTEREST_BONUS per painting by a preferred artist. This is
     * how the "most interesting route" variant works — same algorithm, tweaked weights.
     * When null, pure distance is used.
     *
     * Effective weight is clamped to at least 1.0 so the graph stays valid.
     *
     * @param graph            the gallery graph
     * @param startId          starting room ID
     * @param endId            destination room ID
     * @param avoidRooms       rooms to skip
     * @param preferredArtists set of preferred artist name strings, or null for plain Dijkstra
     * @return optimal path as a list of rooms; empty if unreachable
     */
    private static List<Room> dijkstraSegment(GalleryGraph graph,
                                              String startId,
                                              String endId,
                                              Set<String> avoidRooms,
                                              Set<String> preferredArtists) {
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> parent = new HashMap<>();

        // Min-heap ordered by cumulative distance
        PriorityQueue<RouteState> pq = new PriorityQueue<>(Comparator.comparingDouble(state -> state.distance));

        dist.put(startId, 0.0);
        parent.put(startId, null);
        pq.offer(new RouteState(startId, 0.0));

        while (!pq.isEmpty()) {
            RouteState entry = pq.poll();
            double costSoFar = entry.distance;
            String currentId = entry.roomId;

            // A better path to this room was already found — skip this entry
            if (costSoFar > dist.getOrDefault(currentId, Double.MAX_VALUE)) continue;

            if (currentId.equals(endId)) return reconstructPath(graph, parent, endId);

            for (Edge edge : graph.getNeighbours(currentId)) {
                String neighbourId = edge.getTargetRoomId();
                if (avoidRooms.contains(neighbourId)) continue;

                double effectiveWeight = edge.getDistance();
                if (preferredArtists != null) {
                    effectiveWeight -= interestBonus(graph.getRoom(neighbourId), preferredArtists);
                }
                effectiveWeight = Math.max(effectiveWeight, 1.0); // clamp — can't go negative

                double newDist = costSoFar + effectiveWeight;
                if (newDist < dist.getOrDefault(neighbourId, Double.MAX_VALUE)) {
                    dist.put(neighbourId, newDist);
                    parent.put(neighbourId, currentId);
                    pq.offer(new RouteState(neighbourId, newDist));
                }
            }
        }

        return Collections.emptyList(); // unreachable
    }

    // -------------------------------------------------------------------------
    // 5. Dijkstra's — Most Interesting Route
    // -------------------------------------------------------------------------

    /**
     * Finds the most interesting route using Dijkstra with artist-preference weighting.
     *
     * For each edge leading into room B, the effective cost is:
     *   distance - (INTEREST_BONUS x paintings by preferred artists in B)
     *
     * Rooms with more matching paintings are cheaper to enter, so Dijkstra
     * naturally routes through them unless they're too far out of the way.
     *
     * @param graph            the gallery graph
     * @param startId          starting room ID
     * @param endId            destination room ID
     * @param waypoints        rooms to pass through in order
     * @param avoidRooms       rooms to treat as impassable
     * @param preferredArtists artists the visitor wants to see
     * @return most interesting route as a list of rooms; empty if no route exists
     */
    public static List<Room> findMostInterestingRoute(GalleryGraph graph,
                                                      String startId,
                                                      String endId,
                                                      List<String> waypoints,
                                                      Set<String> avoidRooms,
                                                      List<Artist> preferredArtists) {
        // Convert to a name set for O(1) lookups inside dijkstraSegment
        Set<String> preferredNames = new HashSet<>();
        for (Artist a : preferredArtists) preferredNames.add(a.getName());

        List<String> stops = buildStops(startId, endId, waypoints);
        return chainSegments(graph, stops, avoidRooms,
                (g, a, b, avoid) -> dijkstraSegment(g, a, b, avoid, preferredNames));
    }

    /**
     * Calculates the interest bonus for a room — how much to knock off the
     * edge weight when entering it. One matching painting = INTEREST_BONUS reduction.
     *
     * @param room           the room being evaluated
     * @param preferredNames set of preferred artist name strings
     * @return total discount to apply to the entry edge weight
     */
    private static double interestBonus(Room room, Set<String> preferredNames) {
        if (room == null || preferredNames.isEmpty()) return 0.0;
        double bonus = 0.0;
        for (Painting p : room.getPaintings()) {
            if (preferredNames.contains(p.getArtist())) {
                bonus += INTEREST_BONUS;
            }
        }
        return bonus;
    }

    // -------------------------------------------------------------------------
    // 6. BFS — Pixel Route (image-based)
    // -------------------------------------------------------------------------

    /**
     * Finds the shortest path between two pixel coordinates on the floorplan
     * image using BFS.
     *
     * White (or near-white) pixels are walkable. The search expands
     * 4-directionally (up, down, left, right) from the start pixel until
     * it reaches the end pixel. Each step costs 1 unit.
     *
     * The returned list of int[] pairs are [x, y] pixel coordinates
     * ordered from start to end. The UI draws a line through them on the map.
     *
     * Waypoints and avoidRooms don't apply here — this operates directly
     * on the image with no knowledge of the room graph.
     *
     * @param mapImage   black-and-white floorplan image; walkable pixels must be
     *                   above PIXEL_THRESHOLD brightness
     * @param startPixel starting pixel as [x, y]
     * @param endPixel   destination pixel as [x, y]
     * @return ordered list of [x, y] coordinates forming the path;
     *         empty if no walkable path exists
     */
    public static List<int[]> findPixelRoute(BufferedImage mapImage,
                                             int[] startPixel,
                                             int[] endPixel) {
        int width  = mapImage.getWidth();
        int height = mapImage.getHeight();

        // Pure white = 0xFFFFFF. Near-white pixels are walkable too.
        final int PIXEL_THRESHOLD = 200;

        // parent[y][x] = the pixel [x,y] we arrived from; null = not yet visited
        int[][][] parent  = new int[height][width][];
        boolean[][] visited = new boolean[height][width];

        int sx = startPixel[0], sy = startPixel[1];
        int ex = endPixel[0],   ey = endPixel[1];

        // Sanity checks before we start
        if (!inBounds(sx, sy, width, height) || !inBounds(ex, ey, width, height)) {
            return Collections.emptyList();
        }
        if (!isWalkable(mapImage, sx, sy, PIXEL_THRESHOLD)
                || !isWalkable(mapImage, ex, ey, PIXEL_THRESHOLD)) {
            return Collections.emptyList();
        }

        Queue<int[]> queue = new LinkedList<>();
        visited[sy][sx] = true;
        parent[sy][sx]  = new int[]{-1, -1}; // sentinel: start has no parent
        queue.add(new int[]{sx, sy});

        // 4-directional movement: right, left, down, up
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int cx = current[0], cy = current[1];

            if (cx == ex && cy == ey) {
                return reconstructPixelPath(parent, sx, sy, ex, ey);
            }

            for (int d = 0; d < 4; d++) {
                int nx = cx + dx[d];
                int ny = cy + dy[d];

                if (inBounds(nx, ny, width, height)
                        && !visited[ny][nx]
                        && isWalkable(mapImage, nx, ny, PIXEL_THRESHOLD)) {
                    visited[ny][nx] = true;
                    parent[ny][nx]  = new int[]{cx, cy};
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        return Collections.emptyList(); // no walkable path
    }

    /**
     * Returns true if the pixel at (x, y) is walkable (bright enough).
     * Uses the red channel as a brightness proxy — fine for greyscale images.
     *
     * @param image     the map image
     * @param x         pixel x
     * @param y         pixel y
     * @param threshold minimum brightness value (0-255) to count as walkable
     */
    private static boolean isWalkable(BufferedImage image, int x, int y, int threshold) {
        int rgb = image.getRGB(x, y);
        int red = (rgb >> 16) & 0xFF; // red channel as brightness for greyscale
        return red >= threshold;
    }

    /**
     * Returns true if (x, y) is inside the image bounds.
     */
    private static boolean inBounds(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Reconstructs the pixel path from start to end by following parent
     * pointers back from the destination, then reversing.
     *
     * @param parent 2D array where parent[y][x] = the pixel we came from
     * @param sx     start x
     * @param sy     start y
     * @param ex     end x
     * @param ey     end y
     * @return ordered list of [x, y] pixel coordinates, start to end
     */
    private static List<int[]> reconstructPixelPath(int[][][] parent,
                                                    int sx, int sy,
                                                    int ex, int ey) {
        List<int[]> path = new ArrayList<>();
        int cx = ex, cy = ey;

        while (!(cx == sx && cy == sy)) {
            path.add(new int[]{cx, cy});
            int[] p = parent[cy][cx];
            cx = p[0];
            cy = p[1];
        }
        path.add(new int[]{sx, sy});

        Collections.reverse(path); // flip so it runs start -> end
        return path;
    }

    // -------------------------------------------------------------------------
    // Shared private utilities
    // -------------------------------------------------------------------------

    /**
     * Builds the full list of stops: [startId, wp1, wp2, ..., endId].
     */
    private static List<String> buildStops(String startId, String endId, List<String> waypoints) {
        List<String> stops = new ArrayList<>();
        stops.add(startId);
        if (waypoints != null) stops.addAll(waypoints);
        stops.add(endId);
        return stops;
    }

    /**
     * Functional interface for a single-result segment finder.
     * Lets us pass different algorithm implementations into chainSegments.
     */
    @FunctionalInterface
    private interface SegmentFinder {
        List<Room> find(GalleryGraph graph, String from, String to, Set<String> avoidRooms);
    }

    /** Simple state object for Dijkstra's priority queue. */
    private static final class RouteState {
        private final String roomId;
        private final double distance;

        private RouteState(String roomId, double distance) {
            this.roomId = roomId;
            this.distance = distance;
        }
    }

    /**
     * Runs a SegmentFinder on each consecutive pair of stops and joins
     * the results into one continuous route.
     *
     * The junction room shared between two adjacent segments is included
     * only once in the final path.
     *
     * @param graph      the gallery graph
     * @param stops      ordered room IDs: [start, wp1, ..., end]
     * @param avoidRooms rooms to skip
     * @param finder     the algorithm to apply to each segment
     * @return full concatenated route; empty if any single segment fails
     */
    private static List<Room> chainSegments(GalleryGraph graph,
                                            List<String> stops,
                                            Set<String> avoidRooms,
                                            SegmentFinder finder) {
        List<Room> fullRoute = new ArrayList<>();

        for (int i = 0; i < stops.size() - 1; i++) {
            String from = stops.get(i);
            String to   = stops.get(i + 1);

            List<Room> segment = finder.find(graph, from, to, avoidRooms);

            if (segment.isEmpty()) return Collections.emptyList(); // whole route fails

            if (fullRoute.isEmpty()) {
                fullRoute.addAll(segment);
            } else {
                // First room of this segment is the junction — already in fullRoute
                fullRoute.addAll(segment.subList(1, segment.size()));
            }
        }

        return fullRoute;
    }

    /**
     * Reconstructs a room path from a parent map by walking back from
     * endId to startId (parent = -1), then reversing the result.
     *
     * @param graph    used to look up Room objects by ID
     * @param parent   map of roomId -> parentRoomId; -1 marks the start
     * @param endId    destination room ID
     * @return ordered list of rooms from start to end
     */
    private static List<Room> reconstructPath(GalleryGraph graph,
                                              Map<String, String> parent,
                                              String endId) {
        List<Room> path = new ArrayList<>();
        String current = endId;

        while (current != null) {
            path.add(graph.getRoom(current));
            current = parent.get(current);
        }

        Collections.reverse(path);
        return path;
    }
}