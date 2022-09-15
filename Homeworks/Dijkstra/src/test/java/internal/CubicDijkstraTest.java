package internal;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

public class CubicDijkstraTest {
    private static void doGetAllPoints(
            int[] dims, int[] coords, int curCoordIdx,
            List<int[]> result
    ) {
        assert dims.length == coords.length && dims.length >= 1;
        assert 0 <= curCoordIdx && curCoordIdx <= coords.length;

        if (curCoordIdx == coords.length) {
            var coordsToSave = new int[coords.length];
            System.arraycopy(coords, 0, coordsToSave, 0, coords.length);
            result.add(coordsToSave);
        } else {
            for (
                    coords[curCoordIdx] = 0;
                    coords[curCoordIdx] < dims[curCoordIdx];
                    coords[curCoordIdx]++
            ) {
                assert dims[curCoordIdx] > 0;
                doGetAllPoints(dims, coords, curCoordIdx + 1, result);
            }
        }
    }

    private static List<int[]> getAllPoints(int[] dims) {
        assert dims.length >= 1;
        var result = new ArrayList<int[]>();
        var coords = new int[dims.length];
        doGetAllPoints(dims, coords, 0, result);
        return result;
    }

    public static long getDistance(int[] dims, int[] pointA, int[] pointB) {
        assert dims.length == pointA.length && pointA.length == pointB.length && dims.length >= 1;

        long res = 0L;
        for (int i = 0; i < dims.length; i++) {
            assert 0 <= pointA[i] && pointA[i] < dims[i];
            assert 0 <= pointB[i] && pointB[i] < dims[i];
            res += Math.abs(pointA[i] - pointB[i]);
        }
        return res;
    }


    private void testCubic(int[] dims, BiFunction<List<Map<Integer, Long>>, Integer, Long[]> dijkstra) {
        assert dims.length >= 1;
        var graph = CubicGraphBuilder.cubic(dims);
        var allPoints = getAllPoints(dims);
        assert allPoints.size() == graph.size();

        for (var startPoint : allPoints) {
            assert startPoint.length == dims.length;
            var startIdx = CubicGraphBuilder.coordsToIndex(startPoint, dims);
            var result = dijkstra.apply(graph, startIdx);
            assertEquals(graph.size(), result.length);

            for (int i = 0; i < graph.size(); i++) {
                assertTrue(result[i] >= 0);
                assertEquals(getDistance(dims, allPoints.get(i), startPoint), result[i]);
            }
        }
    }

    @Test
    public void arrayOneDimension() {
        testCubic(new int[]{10}, SequentialDijkstra::array);
    }

    @Test
    public void arrayTwoDimensions() {
        testCubic(new int[]{10, 20}, SequentialDijkstra::array);
    }

    @Test
    public void arrayThreeDimensions() {
        testCubic(new int[]{10, 10, 10}, SequentialDijkstra::array);
    }

    @Test
    public void heapOneDimension() {
        testCubic(new int[]{10}, SequentialDijkstra::heap);
    }

    @Test
    public void heapTwoDimensions() {
        testCubic(new int[]{10, 20}, SequentialDijkstra::heap);
    }

    @Test
    public void heapThreeDimensions() {
        testCubic(new int[]{10, 10, 10}, SequentialDijkstra::heap);
    }
}
