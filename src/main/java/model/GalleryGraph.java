package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom weighted undirected graph representing the main floor of the National Gallery, London.
 *
 * Each Room is a vertex in the graph. Each doorway or corridor connecting two
 * rooms is an undirected weighted edge, where the weight represents the walking distance
 * between the two rooms in pixels (relative to the floorplan image scale).
 *
 * Internal structure
 *   rooms — a HashMap from room ID to Room object, giving
 *       O(1) lookup by ID. This is the vertex set of the graph
 *   adjacencyList — a HashMap from room ID to a list of Edge
 *       objects, giving O(degree) neighbour traversal. This is the edge set of the graph.
 *       Both directions of each edge are stored (undirected graph).
 *
 * Why an adjacency list?
 * The gallery floor has approximately 30–50 rooms, and each room connects to only a
 * small number of neighbours (typically 2–4). This makes the graph sparse. An adjacency
 * list uses space proportional to the number of edges (O(E)), whereas an adjacency matrix
 * would use O(V²) and be mostly empty. The list also allows weighted edges naturally,
 * which are required for Dijkstra's algorithm.
 *
 * XStream compatibility
 * HashMap and ArrayList are serialised by XStream natively. The
 * Edge class is { static so XStream can instantiate it without requiring
 * an enclosingGalleryGraph instance. No annotations are needed.
 */
public class GalleryGraph {

    /**
     * Vertex set: maps each room's unique ID to its {@link Room} object.
     * Provides O(1) room lookup by ID throughout the graph algorithms.
     */
    private Map<Integer, Room> rooms;

    /**
     * Edge set: maps each room's unique ID to the list of edges leaving that room.
     * Because the graph is undirected, every edge (a↔b) is stored twice:
     * once in {@code adjacencyList.get(a)} and once in {@code adjacencyList.get(b)}.
     */
    private Map<Integer, List<Edge>> adjacencyList;

    // -------------------------------------------------------------------------
    // Inner class — Edge
    // -------------------------------------------------------------------------

    /**
     * Represents a single directed half-edge in the adjacency list.
     *
     * <p>Because the graph is undirected, each physical doorway between two rooms
     * is represented as two {@code Edge} objects — one in each direction. This means
     * algorithms can always look up "which rooms can I reach from room X?" by reading
     * {@code adjacencyList.get(X)} without any special-casing.</p>
     *
     * <p>The class is {@code static} so XStream can deserialise it without needing
     * a reference to the enclosing {@link GalleryGraph} instance.</p>
     */
    public static class Edge {

        /**
         * The ID of the room this edge leads to (the destination vertex).
         * Matches a key in {@link GalleryGraph#rooms}.
         */
        private int targetRoomId;

        /**
         * Walking distance from the source room to the target room, measured in pixels
         * relative to the floorplan image. Used as the edge weight in Dijkstra's algorithm.
         * Must be a positive value.
         */
        private double distance;

        /** No-arg constructor required by XStream for XML deserialisation. */
        public Edge() {}

        /**
         * Creates an Edge pointing to the given target room with the given distance.
         *
         * @param targetRoomId ID of the destination room; must exist in the graph
         * @param distance     walking distance in pixels; must be positive
         */
        public Edge(int targetRoomId, double distance) {
            if (distance <= 0) throw new IllegalArgumentException("Edge distance must be positive.");
            this.targetRoomId = targetRoomId;
            this.distance = distance;
        }

        /** @return the ID of the destination room */
        public int getTargetRoomId() { return targetRoomId; }

        /** @param targetRoomId the ID of the destination room */
        public void setTargetRoomId(int targetRoomId) { this.targetRoomId = targetRoomId; }

        /** @return walking distance to the destination room in pixels */
        public double getDistance() { return distance; }

        /** @param distance walking distance in pixels; must be positive */
        public void setDistance(double distance) {
            if (distance <= 0) throw new IllegalArgumentException("Edge distance must be positive.");
            this.distance = distance;
        }

        /** Example output: {@code Edge{targetRoomId=35, distance=52.0}} */
        @Override
        public String toString() {
            return "Edge{targetRoomId=" + targetRoomId + ", distance=" + distance + "}";
        }
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * No-arg constructor required by XStream for XML deserialisation.
     * Initialises both internal maps so they are never null after construction.
     */
    public GalleryGraph() {
        this.rooms = new HashMap<>();
        this.adjacencyList = new HashMap<>();
    }

    // -------------------------------------------------------------------------
    // Graph mutation methods
    // -------------------------------------------------------------------------

    /**
     * Adds a room (vertex) to the graph.
     *
     * <p>If a room with the same ID already exists it is silently replaced.
     * An empty edge list is created for the room if one does not already exist,
     * so it is always safe to call {@link #getNeighbours(int)} on any added room.</p>
     *
     * @param room the room to add; must not be null
     */
    public void addRoom(Room room) {
        if (room == null) throw new IllegalArgumentException("Room must not be null.");
        rooms.put(room.getId(), room);
        // Ensure an edge list exists even for rooms with no connections yet
        adjacencyList.putIfAbsent(room.getId(), new ArrayList<>());
    }

    /**
     * Connects two rooms with an undirected weighted edge representing a doorway
     * or corridor between them.
     *
     * <p>Both rooms must already exist in the graph (added via {@link #addRoom(Room)}).
     * The edge is stored in both directions so that traversal from either room
     * correctly discovers the other as a neighbour.</p>
     *
     * <p>Duplicate edges (same pair of room IDs) are not checked for — the XML
     * database should not define the same connection twice.</p>
     *
     * @param roomIdA  ID of the first room
     * @param roomIdB  ID of the second room
     * @param distance walking distance between the rooms in pixels; must be positive
     * @throws IllegalArgumentException if either room ID does not exist in the graph
     */
    public void connectRooms(int roomIdA, int roomIdB, double distance) {
        if (!rooms.containsKey(roomIdA))
            throw new IllegalArgumentException("Room not found in graph: " + roomIdA);
        if (!rooms.containsKey(roomIdB))
            throw new IllegalArgumentException("Room not found in graph: " + roomIdB);

        // Add edge in both directions (undirected graph)
        adjacencyList.get(roomIdA).add(new Edge(roomIdB, distance));
        adjacencyList.get(roomIdB).add(new Edge(roomIdA, distance));
    }

    // -------------------------------------------------------------------------
    // Graph query methods
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link Room} with the given ID, or {@code null} if not found.
     *
     * @param roomId the unique room identifier
     * @return the Room, or null if no room with that ID exists in the graph
     */
    public Room getRoom(int roomId) {
        return rooms.get(roomId);
    }

    /**
     * Returns an unmodifiable view of all rooms in the graph, keyed by room ID.
     * Algorithms and the UI should use this to iterate over all vertices.
     *
     * @return unmodifiable map of room ID → Room
     */
    public Map<Integer, Room> getAllRooms() {
        return Collections.unmodifiableMap(rooms);
    }

    /**
     * Returns the list of edges (neighbours) for the given room ID.
     *
     * <p>The returned list is the live internal list — do not modify it directly.
     * Returns an empty list (never null) if the room has no connections.</p>
     *
     * @param roomId the unique room identifier
     * @return list of edges from this room, or an empty list if none exist
     */
    public List<Edge> getNeighbours(int roomId) {
        return adjacencyList.getOrDefault(roomId, Collections.emptyList());
    }

    /**
     * Returns {@code true} if a direct edge exists between the two given rooms.
     * Order of arguments does not matter (undirected graph).
     *
     * @param roomIdA ID of the first room
     * @param roomIdB ID of the second room
     * @return true if the two rooms are directly connected by a doorway/corridor
     */
    public boolean areConnected(int roomIdA, int roomIdB) {
        List<Edge> edges = adjacencyList.getOrDefault(roomIdA, Collections.emptyList());
        for (Edge edge : edges)
            if (edge.getTargetRoomId() == roomIdB) return true;
        return false;
    }

    /**
     * Returns {@code true} if a room with the given ID exists in the graph.
     *
     * @param roomId the unique room identifier
     * @return true if the room is present in the graph
     */
    public boolean containsRoom(int roomId) {
        return rooms.containsKey(roomId);
    }

    /**
     * Returns the number of rooms (vertices) in the graph.
     *
     * @return total number of rooms
     */
    public int getRoomCount() {
        return rooms.size();
    }

    /**
     * Returns the number of unique physical edges (doorways/corridors) in the graph.
     *
     * Because each undirected edge is stored twice in the adjacency list (once per
     * direction), the total entry count is divided by two to give the true edge count.
     *
     * @return total number of unique connections between rooms
     */
    public int getEdgeCount() {
        int total = 0;
        for (List<Edge> edges : adjacencyList.values())
            total += edges.size();
        return total / 2; // each undirected edge is stored twice
    }

    // -------------------------------------------------------------------------
    // Standard overrides
    // -------------------------------------------------------------------------

    /**
     * Returns a summary string useful for debugging.
     * Example output: {@code GalleryGraph{rooms=34, edges=41}}
     */
    @Override
    public String toString() {
        return "GalleryGraph{rooms=" + getRoomCount() + ", edges=" + getEdgeCount() + "}";
    }
}