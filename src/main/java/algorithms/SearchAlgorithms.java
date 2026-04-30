package algorithms;

import model.GalleryGraph;
import model.GalleryGraph.Edge;
import model.Artist;
import model.Painting;
import model.Room;

import java.awt.image.BufferedImage;
import java.util.*;

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
 * WAYPOINTS NOTE:
 *   All graph methods expect waypoints to contain ONLY the intermediate stops
 *   between start and end — NOT the start or end themselves. The callers in
 *   MainUIController must pass subList(1, size-1) of the whitelist.
 *   buildStops() assembles the full [start, wp1, wp2, ..., end] list internally.
 */
public class SearchAlgorithms {

    // How much an edge weight is reduced per painting by a preferred artist.
    private static final double INTEREST_BONUS = 30.0;

    // Minimum brightness for a pixel to count as walkable in BFS pixel search.
    private static final int WALKABLE_THRESHOLD = 200;

    // -------------------------------------------------------------------------
    // 1. DFS — Single Route
    // -------------------------------------------------------------------------

    /**
     * Finds any single valid route between two rooms using DFS.
     * Not guaranteed to be shortest — just the first path DFS finds.
     *
     * @param graph      the gallery graph
     * @param startId    starting room ID
     * @param endId      destination room ID
     * @param waypoints  intermediate rooms to pass through (NOT including start/end)
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
     * Returns true once the destination is found.
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
                    return true;
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
     * Finds multiple distinct routes between two rooms using DFS.
     * Stops when maxRoutes is reached or all paths are exhausted.
     *
     * @param graph      the gallery graph
     * @param startId    starting room ID
     * @param endId      destination room ID
     * @param waypoints  intermediate rooms to pass through (NOT including start/end)
     * @param avoidRooms rooms to treat as impassable
     * @param maxRoutes  maximum number of routes to return; must be >= 1
     * @return list of routes, each route being an ordered list of rooms
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
     * Recursively collects all segment permutations across each pair of stops,
     * building up complete routes in allRoutes.
     */
    private static void collectMultipleRoutes(GalleryGraph graph,
                                              List<String> stops,
                                              Set<String> avoidRooms,
                                              int maxRoutes,
                                              List<Room> currentPath,
                                              List<List<Room>> allRoutes) {
        if (stops.size() == 1) {
            if (!currentPath.isEmpty()) {
                allRoutes.add(new ArrayList<>(currentPath));
            }
            return;
        }

        String fromId = stops.get(0);
        String toId   = stops.get(1);

        List<List<Room>> segments = new ArrayList<>();
        dfsMultiple(graph, fromId, toId, new HashSet<>(), new ArrayList<>(),
                avoidRooms, segments, maxRoutes);

        List<String> remainingStops = stops.subList(1, stops.size());

        for (List<Room> seg : segments) {
            if (allRoutes.size() >= maxRoutes) return;

            List<Room> extended = new ArrayList<>(currentPath);

            // Remove duplicate junction room at the segment join point
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
     * Recursive DFS that collects ALL paths to endId, not just the first.
     * Backtracks fully so every route variation is explored.
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
            results.add(new ArrayList<>(path));
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
     * BFS guarantees fewest hops because it explores distance N before N+1.
     *
     * @param graph      the gallery graph
     * @param startId    starting room ID
     * @param endId      destination room ID
     * @param waypoints  intermediate rooms to pass through (NOT including start/end)
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
     * BFS between two rooms. Tracks parents so the path can be reconstructed.
     */
    private static List<Room> bfsSegment(GalleryGraph graph,
                                         String startId,
                                         String endId,
                                         Set<String> avoidRooms) {
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

        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // 4. Dijkstra — Shortest Route (minimum walking distance)
    // -------------------------------------------------------------------------

    /**
     * Finds the shortest route by total walking distance using Dijkstra's algorithm.
     * Uses a min-heap; first time the destination is popped is guaranteed optimal
     * because all edge weights are positive.
     *
     * @param graph      the gallery graph
     * @param startId    starting room ID
     * @param endId      destination room ID
     * @param waypoints  intermediate rooms to pass through (NOT including start/end)
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
     * Core Dijkstra implementation for one segment.
     *
     * When preferredArtists is non-null, the effective edge weight into a room is
     * reduced by INTEREST_BONUS per matching painting — this is how the "most
     * interesting route" variant works. Weight is clamped to >= 1.0.
     *
     * @param preferredArtists set of preferred artist name strings, or null for plain Dijkstra
     */
    private static List<Room> dijkstraSegment(GalleryGraph graph,
                                              String startId,
                                              String endId,
                                              Set<String> avoidRooms,
                                              Set<String> preferredArtists) {
        Map<String, Double> dist   = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        PriorityQueue<RouteState> pq =
                new PriorityQueue<>(Comparator.comparingDouble(s -> s.distance));

        dist.put(startId, 0.0);
        parent.put(startId, null);
        pq.offer(new RouteState(startId, 0.0));

        while (!pq.isEmpty()) {
            RouteState entry = pq.poll();
            String currentId  = entry.roomId;
            double costSoFar  = entry.distance;

            // Stale entry — a cheaper path was already found
            if (costSoFar > dist.getOrDefault(currentId, Double.MAX_VALUE)) continue;

            if (currentId.equals(endId)) return reconstructPath(graph, parent, endId);

            for (Edge edge : graph.getNeighbours(currentId)) {
                String neighbourId = edge.getTargetRoomId();
                if (avoidRooms.contains(neighbourId)) continue;

                double weight = edge.getDistance();
                if (preferredArtists != null) {
                    weight -= interestBonus(graph.getRoom(neighbourId), preferredArtists);
                }
                weight = Math.max(weight, 1.0); // never let weight go negative

                double newDist = costSoFar + weight;
                if (newDist < dist.getOrDefault(neighbourId, Double.MAX_VALUE)) {
                    dist.put(neighbourId, newDist);
                    parent.put(neighbourId, currentId);
                    pq.offer(new RouteState(neighbourId, newDist));
                }
            }
        }

        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // 5. Dijkstra — Most Interesting Route
    // -------------------------------------------------------------------------

    /**
     * Finds the most interesting route using Dijkstra with artist-preference weighting.
     *
     * For each edge leading into room B:
     *   effective cost = distance - (INTEREST_BONUS x paintings by preferred artists in B)
     *
     * Rooms with more matching paintings become cheaper to enter, so Dijkstra
     * naturally routes through them unless they are too far out of the way.
     *
     * @param graph            the gallery graph
     * @param startId          starting room ID
     * @param endId            destination room ID
     * @param waypoints        intermediate rooms to pass through (NOT including start/end)
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
        Set<String> preferredNames = new HashSet<>();
        for (Artist a : preferredArtists) preferredNames.add(a.getName());

        List<String> stops = buildStops(startId, endId, waypoints);
        return chainSegments(graph, stops, avoidRooms,
                (g, a, b, avoid) -> dijkstraSegment(g, a, b, avoid, preferredNames));
    }

    /**
     * Calculates the interest bonus for entering a room.
     * One matching painting reduces the entry edge weight by INTEREST_BONUS.
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
     * Finds the shortest walkable path between two pixel coordinates on the
     * floorplan image using BFS.
     *
     * White/near-white pixels (brightness >= WALKABLE_THRESHOLD) are walkable.
     * Expands 4-directionally. Each step costs 1 unit.
     *
     * The returned list contains [x, y] pixel coordinates ordered start to end.
     * Waypoints and avoidRooms do not apply here — this operates on raw pixels.
     *
     * @param mapImage   black-and-white floorplan; walkable pixels must be bright
     * @param startPixel starting pixel as [x, y]
     * @param endPixel   destination pixel as [x, y]
     * @return ordered list of [x, y] coordinates forming the path; empty if unreachable
     */
    public static List<int[]> findPixelRoute(BufferedImage mapImage,
                                             int[] startPixel,
                                             int[] endPixel) {
        int width  = mapImage.getWidth();
        int height = mapImage.getHeight();
        int sx = startPixel[0], sy = startPixel[1];
        int ex = endPixel[0],   ey = endPixel[1];

        if (!inBounds(sx, sy, width, height) || !inBounds(ex, ey, width, height))
            return Collections.emptyList();
        if (!isWalkable(mapImage, sx, sy) || !isWalkable(mapImage, ex, ey))
            return Collections.emptyList();
        if (sx == ex && sy == ey)
            return List.of(new int[]{sx, sy});

        // parentX/parentY store where each pixel was discovered from; -1 = unvisited
        int[] parentX = new int[width * height];
        int[] parentY = new int[width * height];
        Arrays.fill(parentX, -1);

        Queue<Integer> queue = new LinkedList<>();
        int startIdx = sy * width + sx;
        parentX[startIdx] = sx; // self-parent marks the start
        parentY[startIdx] = sy;
        queue.add(startIdx);

        int[] dx = {1, -1, 0,  0};
        int[] dy = {0,  0, 1, -1};
        boolean found = false;

        outer:
        while (!queue.isEmpty()) {
            int idx = queue.poll();
            int cx  = idx % width;
            int cy  = idx / width;

            for (int d = 0; d < 4; d++) {
                int nx = cx + dx[d];
                int ny = cy + dy[d];
                if (!inBounds(nx, ny, width, height)) continue;
                int nIdx = ny * width + nx;
                if (parentX[nIdx] != -1) continue;          // already visited
                if (!isWalkable(mapImage, nx, ny)) continue;
                parentX[nIdx] = cx;
                parentY[nIdx] = cy;
                if (nx == ex && ny == ey) { found = true; break outer; }
                queue.add(nIdx);
            }
        }

        if (!found) return Collections.emptyList();

        // Reconstruct path end -> start, then reverse
        List<int[]> path = new ArrayList<>();
        int cx = ex, cy = ey;
        while (!(cx == sx && cy == sy)) {
            path.add(new int[]{cx, cy});
            int idx = cy * width + cx;
            int px = parentX[idx];
            int py = parentY[idx];
            cx = px;
            cy = py;
        }
        path.add(new int[]{sx, sy});
        Collections.reverse(path);
        return path;
    }

    /**
     * Simplifies a dense pixel path using Douglas-Peucker.
     * epsilon=2.0 is a good default — raise for smoother lines,
     * lower to preserve tight corridor bends.
     */
    public static List<int[]> simplifyPath(List<int[]> path, double epsilon) {
        if (path.size() < 3) return path;
        return douglasPeucker(path, 0, path.size() - 1, epsilon);
    }

    private static List<int[]> douglasPeucker(List<int[]> pts, int start, int end, double eps) {
        if (end <= start + 1) {
            List<int[]> result = new ArrayList<>();
            result.add(pts.get(start));
            result.add(pts.get(end));
            return result;
        }

        double maxDist = 0;
        int    maxIdx  = start;
        int[]  a       = pts.get(start);
        int[]  b       = pts.get(end);

        for (int i = start + 1; i < end; i++) {
            double d = perpendicularDistance(pts.get(i), a, b);
            if (d > maxDist) { maxDist = d; maxIdx = i; }
        }

        if (maxDist > eps) {
            List<int[]> left  = douglasPeucker(pts, start,  maxIdx, eps);
            List<int[]> right = douglasPeucker(pts, maxIdx, end,    eps);
            List<int[]> merged = new ArrayList<>(left);
            merged.addAll(right.subList(1, right.size())); // drop duplicate junction
            return merged;
        }

        List<int[]> result = new ArrayList<>();
        result.add(a);
        result.add(b);
        return result;
    }

    private static double perpendicularDistance(int[] p, int[] a, int[] b) {
        double ax = a[0], ay = a[1], bx = b[0], by = b[1];
        double px = p[0], py = p[1];
        double dx = bx - ax, dy = by - ay;
        if (dx == 0 && dy == 0) return Math.hypot(px - ax, py - ay);
        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    /**
     * Returns true if the pixel at (x, y) is walkable (bright enough).
     * Uses the red channel as brightness — correct for greyscale images.
     */
    private static boolean isWalkable(BufferedImage image, int x, int y) {
        int rgb = image.getRGB(x, y);
        int red = (rgb >> 16) & 0xFF;
        return red >= WALKABLE_THRESHOLD;
    }

    /** Returns true if (x, y) is inside the image bounds. */
    private static boolean inBounds(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    // -------------------------------------------------------------------------
    // Shared private utilities
    // -------------------------------------------------------------------------

    /**
     * Builds the full stop list: [startId, wp1, wp2, ..., endId].
     * waypoints must NOT include start or end — they are added here.
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

    /** State object for Dijkstra's priority queue. */
    private static final class RouteState {
        final String roomId;
        final double distance;

        RouteState(String roomId, double distance) {
            this.roomId   = roomId;
            this.distance = distance;
        }
    }

    /**
     * Runs a SegmentFinder on each consecutive pair of stops and joins the
     * results into one continuous route. The shared junction room between
     * adjacent segments appears only once in the final path.
     * Returns an empty list immediately if any single segment has no route.
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
                // First room of segment is the junction — already the last room in fullRoute
                fullRoute.addAll(segment.subList(1, segment.size()));
            }
        }

        return fullRoute;
    }

    /**
     * Reconstructs a room path from a parent map by walking back from endId
     * to the start (where parent value is null), then reversing.
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