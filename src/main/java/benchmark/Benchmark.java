package benchmark;

import algorithms.SearchAlgorithms;
import model.GalleryGraph;
import model.GalleryLoader;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Measurement(iterations=10)
@Warmup(iterations=5)
@Fork(value=1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class Benchmark {
    private final GalleryLoader galleryLoader = new GalleryLoader();
    private final GalleryGraph graph = galleryLoader.getGraph();
    private final List<String> selectedRooms = new ArrayList<>();
    // 61 -> 21 -> 41 is a pretty long path

    @Setup(Level.Trial)
    public void setup() throws Exception {
        selectedRooms.add("61");
        selectedRooms.add("21");
        selectedRooms.add("41");
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void BFS(Blackhole bh) {
        bh.consume(SearchAlgorithms.findShortestRouteBFS(graph, "61", "41", selectedRooms, null));
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void testDFS(Blackhole bh) {
        bh.consume(SearchAlgorithms.findMultipleRoutes(graph, "61", "41", selectedRooms, null, 1000));
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void testDijkstraShortest(Blackhole bh) {
        bh.consume(SearchAlgorithms.findShortestRouteDijkstra(graph, "61", "41", selectedRooms, null));
    }

    public static void main(String[] args) throws RunnerException, IOException {
        Main.main(args);
    }
}
