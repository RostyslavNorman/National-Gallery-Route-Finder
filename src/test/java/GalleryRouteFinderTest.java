import algorithms.SearchAlgorithms;
import model.Artist;
import model.GalleryGraph;
import model.Painting;
import model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("National Gallery Route Finder — Backend Tests")
class GalleryRouteFinderTest {

    private GalleryGraph graph;

    // String constants — match XML ids exactly
    private static final String ROOM_1 = "1";
    private static final String ROOM_2 = "2";
    private static final String ROOM_3 = "3";
    private static final String ROOM_4 = "4";
    private static final String ROOM_5 = "5";
    private static final String ROOM_6 = "6";

    @BeforeEach
    void buildTestGraph() {
        graph = new GalleryGraph();

        graph.addRoom(new Room(ROOM_1, "Room 1", 100, 100));
        graph.addRoom(new Room(ROOM_2, "Room 2", 200, 100));
        graph.addRoom(new Room(ROOM_3, "Room 3", 300, 100));
        graph.addRoom(new Room(ROOM_4, "Room 4", 100, 200));
        graph.addRoom(new Room(ROOM_5, "Room 5", 200, 200));
        graph.addRoom(new Room(ROOM_6, "Room 6", 300, 200));

        graph.getRoom(ROOM_3).addPainting(
                new Painting("A Lady seated at a Virginal", "Johannes Vermeer")
        );
        graph.getRoom(ROOM_6).addPainting(
                new Painting("Water-Lilies", "Claude Monet")
        );

        graph.connectRooms(ROOM_1, ROOM_2, 50);
        graph.connectRooms(ROOM_2, ROOM_3, 50);
        graph.connectRooms(ROOM_3, ROOM_6, 40);
        graph.connectRooms(ROOM_1, ROOM_4, 60);
        graph.connectRooms(ROOM_4, ROOM_5, 30);
        graph.connectRooms(ROOM_5, ROOM_6, 30);
    }

    @Nested
    @DisplayName("GalleryGraph — Custom Graph Data Structure")
    class GalleryGraphTests {

        @Test
        @DisplayName("addRoom: graph reports correct room count after adding rooms")
        void testRoomCount() {
            assertEquals(6, graph.getRoomCount());
        }

        @Test
        @DisplayName("addRoom: containsRoom returns true for an added room")
        void testContainsRoomTrue() {
            assertTrue(graph.containsRoom(ROOM_1));
        }

        @Test
        @DisplayName("addRoom: containsRoom returns false for a room not in the graph")
        void testContainsRoomFalse() {
            assertFalse(graph.containsRoom("99"));
        }

        @Test
        @DisplayName("addRoom: getRoom returns the correct Room object by ID")
        void testGetRoomReturnsCorrectRoom() {
            Room room = graph.getRoom(ROOM_3);
            assertNotNull(room);
            assertEquals(ROOM_3, room.getId());
            assertEquals("Room 3", room.getName());
        }

        @Test
        @DisplayName("addRoom: getRoom returns null for an unknown room ID")
        void testGetRoomUnknown() {
            assertNull(graph.getRoom("99"));
        }

        @Test
        @DisplayName("addRoom: replacing a room with the same ID updates the vertex")
        void testAddRoomOverwritesExisting() {
            graph.addRoom(new Room(ROOM_1, "Room 1 Updated", 0, 0));
            assertEquals("Room 1 Updated", graph.getRoom(ROOM_1).getName());
            assertEquals(6, graph.getRoomCount());
        }

        @Test
        @DisplayName("addRoom: throws when null is passed")
        void testAddNullRoomThrows() {
            assertThrows(IllegalArgumentException.class, () -> graph.addRoom(null));
        }

        @Test
        @DisplayName("connectRooms: graph reports correct edge count")
        void testEdgeCount() {
            assertEquals(6, graph.getEdgeCount());
        }

        @Test
        @DisplayName("connectRooms: areConnected returns true for directly connected rooms")
        void testAreConnectedTrue() {
            assertTrue(graph.areConnected(ROOM_1, ROOM_2));
        }

        @Test
        @DisplayName("connectRooms: areConnected is symmetric (undirected graph)")
        void testAreConnectedSymmetric() {
            assertTrue(graph.areConnected(ROOM_2, ROOM_1));
        }

        @Test
        @DisplayName("connectRooms: areConnected returns false for unconnected rooms")
        void testAreConnectedFalse() {
            assertFalse(graph.areConnected(ROOM_1, ROOM_6));
        }

        @Test
        @DisplayName("connectRooms: getNeighbours returns correct neighbours for Room 1")
        void testGetNeighboursRoom1() {
            List<GalleryGraph.Edge> neighbours = graph.getNeighbours(ROOM_1);
            assertEquals(2, neighbours.size());
            Set<String> ids = new HashSet<>();
            for (GalleryGraph.Edge e : neighbours) ids.add(e.getTargetRoomId());
            assertTrue(ids.contains(ROOM_2));
            assertTrue(ids.contains(ROOM_4));
        }

        @Test
        @DisplayName("connectRooms: getNeighbours returns correct distance for edge 1-2")
        void testEdgeDistanceRoom1To2() {
            List<GalleryGraph.Edge> neighbours = graph.getNeighbours(ROOM_1);
            double dist = -1;
            for (GalleryGraph.Edge e : neighbours) {
                if (e.getTargetRoomId().equals(ROOM_2)) dist = e.getDistance();
            }
            assertEquals(50.0, dist, 0.001);
        }

        @Test
        @DisplayName("connectRooms: throws when connecting a room not in the graph")
        void testConnectUnknownRoomThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> graph.connectRooms(ROOM_1, "99", 50));
        }

        @Test
        @DisplayName("connectRooms: throws when distance is zero or negative")
        void testConnectNegativeDistanceThrows() {
            graph.addRoom(new Room("7", "Room 7", 400, 100));
            assertThrows(IllegalArgumentException.class,
                    () -> graph.connectRooms(ROOM_1, "7", 0));
        }

        @Test
        @DisplayName("getAllRooms: returns all six rooms")
        void testGetAllRoomsSize() {
            assertEquals(6, graph.getAllRooms().size());
        }

        @Test
        @DisplayName("getAllRooms: returned map is unmodifiable")
        void testGetAllRoomsUnmodifiable() {
            assertThrows(UnsupportedOperationException.class,
                    () -> graph.getAllRooms().put("99", new Room("99", "Fake", 0, 0)));
        }
    }

    @Nested
    @DisplayName("SearchAlgorithms — Routing Algorithms")
    class SearchAlgorithmsTests {

        // String versions of the empty collections
        private final List<String> NO_WAYPOINTS = Collections.emptyList();
        private final Set<String>  NO_AVOID     = Collections.emptySet();

        @Nested
        @DisplayName("DFS — Single Route")
        class DfsSingleTests {

            @Test
            @DisplayName("returns a non-empty route from Room 1 to Room 6")
            void testFindsARoute() {
                List<Room> route = SearchAlgorithms.findSingleRoute(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID);
                assertFalse(route.isEmpty());
            }

            @Test
            @DisplayName("route starts at Room 1 and ends at Room 6")
            void testStartAndEnd() {
                List<Room> route = SearchAlgorithms.findSingleRoute(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID);
                assertEquals(ROOM_1, route.get(0).getId());
                assertEquals(ROOM_6, route.get(route.size() - 1).getId());
            }

            @Test
            @DisplayName("every room in the route is connected to the next")
            void testRouteIsValid() {
                List<Room> route = SearchAlgorithms.findSingleRoute(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID);
                for (int i = 0; i < route.size() - 1; i++) {
                    String a = route.get(i).getId();
                    String b = route.get(i + 1).getId();
                    assertTrue(graph.areConnected(a, b),
                            "Expected rooms " + a + " and " + b + " to be connected");
                }
            }

            @Test
            @DisplayName("route contains no duplicate rooms (no cycles)")
            void testNoDuplicateRooms() {
                List<Room> route = SearchAlgorithms.findSingleRoute(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID);
                Set<String> seen = new HashSet<>();
                for (Room r : route) {
                    assertTrue(seen.add(r.getId()),
                            "Room " + r.getId() + " appears more than once in route");
                }
            }

            @Test
            @DisplayName("returns empty list when destination is unreachable")
            void testUnreachableDestination() {
                graph.addRoom(new Room("99", "Isolated", 999, 999));
                List<Room> route = SearchAlgorithms.findSingleRoute(
                        graph, ROOM_1, "99", NO_WAYPOINTS, NO_AVOID);
                assertTrue(route.isEmpty());
            }

            @Test
            @DisplayName("returns single-room list when start equals destination")
            void testStartEqualsEnd() {
                List<Room> route = SearchAlgorithms.findSingleRoute(
                        graph, ROOM_1, ROOM_1, NO_WAYPOINTS, NO_AVOID);
                assertEquals(1, route.size());
                assertEquals(ROOM_1, route.get(0).getId());
            }

            @Test
            @DisplayName("avoids specified rooms")
            void testAvoidsRooms() {
                Set<String> avoid = new HashSet<>();
                avoid.add(ROOM_4);
                avoid.add(ROOM_5);
                List<Room> route = SearchAlgorithms.findSingleRoute(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, avoid);
                assertFalse(route.isEmpty());
                for (Room r : route) {
                    assertFalse(avoid.contains(r.getId()),
                            "Route should not contain avoided room " + r.getId());
                }
            }

            @Test
            @DisplayName("returns empty list when all routes are blocked by avoidRooms")
            void testAllRoutesBlocked() {
                Set<String> avoid = new HashSet<>();
                avoid.add(ROOM_2);
                avoid.add(ROOM_4);
                List<Room> route = SearchAlgorithms.findSingleRoute(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, avoid);
                assertTrue(route.isEmpty());
            }

            @Test
            @DisplayName("visits waypoint room on the route")
            void testVisitsWaypoint() {
                List<String> waypoints = List.of(ROOM_3);
                List<Room> route = SearchAlgorithms.findSingleRoute(
                        graph, ROOM_1, ROOM_6, waypoints, NO_AVOID);
                assertFalse(route.isEmpty());
                boolean containsWaypoint = route.stream()
                        .anyMatch(r -> r.getId().equals(ROOM_3));
                assertTrue(containsWaypoint, "Route should pass through waypoint Room 3");
            }
        }

        @Nested
        @DisplayName("DFS — Multiple Routes")
        class DfsMultipleTests {

            @Test
            @DisplayName("finds more than one route from Room 1 to Room 6")
            void testFindsMultipleRoutes() {
                List<List<Room>> routes = SearchAlgorithms.findMultipleRoutes(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID, 10);
                assertTrue(routes.size() > 1,
                        "Expected multiple routes but found: " + routes.size());
            }

            @Test
            @DisplayName("does not exceed the maxRoutes limit")
            void testRespectsMaxRoutes() {
                int max = 2;
                List<List<Room>> routes = SearchAlgorithms.findMultipleRoutes(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID, max);
                assertTrue(routes.size() <= max);
            }

            @Test
            @DisplayName("every returned route starts at Room 1 and ends at Room 6")
            void testAllRoutesStartAndEnd() {
                List<List<Room>> routes = SearchAlgorithms.findMultipleRoutes(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID, 10);
                for (List<Room> route : routes) {
                    assertEquals(ROOM_1, route.get(0).getId());
                    assertEquals(ROOM_6, route.get(route.size() - 1).getId());
                }
            }

            @Test
            @DisplayName("every room in every route is connected to the next")
            void testAllRoutesAreValid() {
                List<List<Room>> routes = SearchAlgorithms.findMultipleRoutes(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID, 10);
                for (List<Room> route : routes) {
                    for (int i = 0; i < route.size() - 1; i++) {
                        String a = route.get(i).getId();
                        String b = route.get(i + 1).getId();
                        assertTrue(graph.areConnected(a, b),
                                "Rooms " + a + " and " + b + " are not connected");
                    }
                }
            }

            @Test
            @DisplayName("throws when maxRoutes is less than 1")
            void testMaxRoutesValidation() {
                assertThrows(IllegalArgumentException.class, () ->
                        SearchAlgorithms.findMultipleRoutes(
                                graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID, 0));
            }
        }

        @Nested
        @DisplayName("BFS — Shortest Route (fewest rooms)")
        class BfsTests {

            @Test
            @DisplayName("finds a route from Room 1 to Room 6")
            void testFindsRoute() {
                List<Room> route = SearchAlgorithms.findShortestRouteBFS(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID);
                assertFalse(route.isEmpty());
            }

            @Test
            @DisplayName("route starts at Room 1 and ends at Room 6")
            void testStartAndEnd() {
                List<Room> route = SearchAlgorithms.findShortestRouteBFS(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID);
                assertEquals(ROOM_1, route.get(0).getId());
                assertEquals(ROOM_6, route.get(route.size() - 1).getId());
            }

            @Test
            @DisplayName("BFS finds a 4-room path (optimal hop count) from Room 1 to Room 6")
            void testOptimalHopCount() {
                List<Room> route = SearchAlgorithms.findShortestRouteBFS(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID);
                assertEquals(4, route.size());
            }

            @Test
            @DisplayName("BFS finds the direct 2-room path for directly connected rooms")
            void testDirectNeighbour() {
                List<Room> route = SearchAlgorithms.findShortestRouteBFS(
                        graph, ROOM_1, ROOM_2, NO_WAYPOINTS, NO_AVOID);
                assertEquals(2, route.size());
                assertEquals(ROOM_1, route.get(0).getId());
                assertEquals(ROOM_2, route.get(1).getId());
            }

            @Test
            @DisplayName("returns empty list when destination is unreachable")
            void testUnreachable() {
                graph.addRoom(new Room("99", "Isolated", 999, 999));
                List<Room> route = SearchAlgorithms.findShortestRouteBFS(
                        graph, ROOM_1, "99", NO_WAYPOINTS, NO_AVOID);
                assertTrue(route.isEmpty());
            }

            @Test
            @DisplayName("avoids specified rooms")
            void testAvoidsRooms() {
                Set<String> avoid = new HashSet<>();
                avoid.add(ROOM_4);
                avoid.add(ROOM_5);
                List<Room> route = SearchAlgorithms.findShortestRouteBFS(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, avoid);
                for (Room r : route) {
                    assertFalse(avoid.contains(r.getId()));
                }
            }

            @Test
            @DisplayName("visits waypoint on the route")
            void testWaypoint() {
                List<String> waypoints = List.of(ROOM_5);
                List<Room> route = SearchAlgorithms.findShortestRouteBFS(
                        graph, ROOM_1, ROOM_6, waypoints, NO_AVOID);
                assertTrue(route.stream().anyMatch(r -> r.getId().equals(ROOM_5)),
                        "Route should pass through waypoint Room 5");
            }
        }

        @Nested
        @DisplayName("Dijkstra's — Shortest Route (minimum distance)")
        class DijkstraShortestTests {

            @Test
            @DisplayName("finds a route from Room 1 to Room 6")
            void testFindsRoute() {
                List<Room> route = SearchAlgorithms.findShortestRouteDijkstra(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID);
                assertFalse(route.isEmpty());
            }

            @Test
            @DisplayName("route starts at Room 1 and ends at Room 6")
            void testStartAndEnd() {
                List<Room> route = SearchAlgorithms.findShortestRouteDijkstra(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID);
                assertEquals(ROOM_1, route.get(0).getId());
                assertEquals(ROOM_6, route.get(route.size() - 1).getId());
            }

            @Test
            @DisplayName("Dijkstra finds path 1-4-5-6 (dist 120) not 1-2-3-6 (dist 140)")
            void testChoosesMinimumDistancePath() {
                List<Room> route = SearchAlgorithms.findShortestRouteDijkstra(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID);
                List<String> ids = route.stream().map(Room::getId).toList();
                assertTrue(ids.contains(ROOM_4) && ids.contains(ROOM_5),
                        "Expected shortest path 1-4-5-6 but got: " + ids);
            }

            @Test
            @DisplayName("Dijkstra finds path 1-2-3 (dist 100) not 1-4-5-6-3 (dist 160)")
            void testShortestPathTo3() {
                List<Room> route = SearchAlgorithms.findShortestRouteDijkstra(
                        graph, ROOM_1, ROOM_3, NO_WAYPOINTS, NO_AVOID);
                List<String> ids = route.stream().map(Room::getId).toList();
                assertTrue(ids.contains(ROOM_2),
                        "Expected path through Room 2 but got: " + ids);
                assertFalse(ids.contains(ROOM_4),
                        "Path should not go through Room 4: " + ids);
            }

            @Test
            @DisplayName("returns empty list when destination is unreachable")
            void testUnreachable() {
                graph.addRoom(new Room("99", "Isolated", 999, 999));
                List<Room> route = SearchAlgorithms.findShortestRouteDijkstra(
                        graph, ROOM_1, "99", NO_WAYPOINTS, NO_AVOID);
                assertTrue(route.isEmpty());
            }

            @Test
            @DisplayName("avoids specified rooms")
            void testAvoidsRooms() {
                Set<String> avoid = new HashSet<>();
                avoid.add(ROOM_4);
                avoid.add(ROOM_5);
                List<Room> route = SearchAlgorithms.findShortestRouteDijkstra(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, avoid);
                assertFalse(route.isEmpty());
                for (Room r : route) assertFalse(avoid.contains(r.getId()));
            }

            @Test
            @DisplayName("visits waypoint on the route")
            void testWaypoint() {
                List<String> waypoints = List.of(ROOM_2);
                List<Room> route = SearchAlgorithms.findShortestRouteDijkstra(
                        graph, ROOM_1, ROOM_6, waypoints, NO_AVOID);
                assertTrue(route.stream().anyMatch(r -> r.getId().equals(ROOM_2)));
            }

            @Test
            @DisplayName("every room in the route is connected to the next")
            void testRouteIsValid() {
                List<Room> route = SearchAlgorithms.findShortestRouteDijkstra(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID);
                for (int i = 0; i < route.size() - 1; i++) {
                    assertTrue(graph.areConnected(
                            route.get(i).getId(), route.get(i + 1).getId()));
                }
            }
        }

        @Nested
        @DisplayName("Dijkstra's — Most Interesting Route")
        class DijkstraMostInterestingTests {

            @Test
            @DisplayName("prefers route through Room 3 when visitor likes Vermeer")
            void testPrefersVermeerRoom() {
                List<Artist> preferred = List.of(new Artist("Johannes Vermeer"));
                List<Room> route = SearchAlgorithms.findMostInterestingRoute(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID, preferred);
                assertFalse(route.isEmpty());
                boolean passesRoom3 = route.stream()
                        .anyMatch(r -> r.getId().equals(ROOM_3));
                assertTrue(passesRoom3,
                        "Route should pass through Room 3 (Vermeer) but was: "
                                + route.stream().map(Room::getId).toList());
            }

            @Test
            @DisplayName("returns the standard shortest route when no artists are preferred")
            void testNoPreferenceFallsBackToShortest() {
                List<Room> interesting = SearchAlgorithms.findMostInterestingRoute(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID, Collections.emptyList());
                List<Room> shortest = SearchAlgorithms.findShortestRouteDijkstra(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID);
                assertEquals(
                        shortest.stream().map(Room::getId).toList(),
                        interesting.stream().map(Room::getId).toList()
                );
            }

            @Test
            @DisplayName("route starts at Room 1 and ends at Room 6")
            void testStartAndEnd() {
                List<Artist> preferred = List.of(new Artist("Claude Monet"));
                List<Room> route = SearchAlgorithms.findMostInterestingRoute(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID, preferred);
                assertFalse(route.isEmpty());
                assertEquals(ROOM_1, route.get(0).getId());
                assertEquals(ROOM_6, route.get(route.size() - 1).getId());
            }

            @Test
            @DisplayName("every room in the route is connected to the next")
            void testRouteIsValid() {
                List<Artist> preferred = List.of(new Artist("Johannes Vermeer"));
                List<Room> route = SearchAlgorithms.findMostInterestingRoute(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, NO_AVOID, preferred);
                for (int i = 0; i < route.size() - 1; i++) {
                    assertTrue(graph.areConnected(
                            route.get(i).getId(), route.get(i + 1).getId()));
                }
            }

            @Test
            @DisplayName("returns empty list when destination is unreachable")
            void testUnreachable() {
                graph.addRoom(new Room("99", "Isolated", 999, 999));
                List<Artist> preferred = List.of(new Artist("Rembrandt van Rijn"));
                List<Room> route = SearchAlgorithms.findMostInterestingRoute(
                        graph, ROOM_1, "99", NO_WAYPOINTS, NO_AVOID, preferred);
                assertTrue(route.isEmpty());
            }

            @Test
            @DisplayName("avoids specified rooms even when they contain preferred paintings")
            void testAvoidsRoomsEvenWithPreferredArtist() {
                Set<String> avoid = new HashSet<>();
                avoid.add(ROOM_3);
                List<Artist> preferred = List.of(new Artist("Johannes Vermeer"));
                List<Room> route = SearchAlgorithms.findMostInterestingRoute(
                        graph, ROOM_1, ROOM_6, NO_WAYPOINTS, avoid, preferred);
                for (Room r : route) {
                    assertNotEquals(ROOM_3, r.getId(),
                            "Avoided Room 3 should not appear in the route");
                }
            }
        }

        @Nested
        @DisplayName("Model — Room and Painting")
        class ModelTests {

            @Test
            @DisplayName("Room.addPainting: painting is retrievable after being added")
            void testAddPainting() {
                Room room = new Room("10", "Test Room", 0, 0);
                Painting p = new Painting("Sunflowers", "Vincent van Gogh");
                room.addPainting(p);
                assertEquals(1, room.getPaintings().size());
                assertEquals("Sunflowers", room.getPaintings().get(0).getTitle());
            }

            @Test
            @DisplayName("Room.addPainting: throws when null painting is added")
            void testAddNullPainting() {
                Room room = new Room("10", "Test Room", 0, 0);
                assertThrows(IllegalArgumentException.class, () -> room.addPainting(null));
            }

            @Test
            @DisplayName("Room.equals: two rooms with the same ID are equal")
            void testRoomEquality() {
                Room a = new Room("5", "Room A", 100, 100);
                Room b = new Room("5", "Room B", 200, 200);
                assertEquals(a, b);
            }

            @Test
            @DisplayName("Room.equals: two rooms with different IDs are not equal")
            void testRoomInequality() {
                Room a = new Room("5", "Room A", 0, 0);
                Room b = new Room("6", "Room B", 0, 0);
                assertNotEquals(a, b);
            }

            @Test
            @DisplayName("Painting: constructor throws when title is null")
            void testPaintingNullTitle() {
                assertThrows(IllegalArgumentException.class,
                        () -> new Painting(null, "Vermeer"));
            }

            @Test
            @DisplayName("Painting: constructor throws when artist is null")
            void testPaintingNullArtist() {
                assertThrows(IllegalArgumentException.class,
                        () -> new Painting("A title", null));
            }

            @Test
            @DisplayName("Artist.equals: two artists with the same name are equal")
            void testArtistEquality() {
                Artist a = new Artist("Rembrandt van Rijn");
                Artist b = new Artist("Rembrandt van Rijn");
                assertEquals(a, b);
            }

            @Test
            @DisplayName("Artist: constructor throws when name is null")
            void testArtistNullName() {
                assertThrows(IllegalArgumentException.class, () -> new Artist(null));
            }
        }
    }
}