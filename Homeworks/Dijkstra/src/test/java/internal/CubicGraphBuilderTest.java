package internal;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CubicGraphBuilderTest {
    @Test
    public void indexOneDimension() {
        int[] dims = {10};
        for (var i = 0; i < 10; i++) {
            assertEquals(i, CubicGraphBuilder.coordsToIndex(new int[]{i}, dims));
        }
    }

    @Test
    public void indexTwoDimensions() {
        int[] dims = {10, 20};
        var used = new HashSet<Integer>();
        for (var i = 0; i < 10; i++) {
            for (var j = 0; j < 20; j++) {
                var res = CubicGraphBuilder.coordsToIndex(new int[]{i, j}, dims);
                assertEquals(i * 20 + j, res);
                var insertRes = used.add(res);
                assertTrue(insertRes);
            }
        }
    }

    @Test
    public void indexThreeDimensions() {
        int[] dims = {10, 20, 30};
        var used = new HashSet<Integer>();
        for (var i = 0; i < 10; i++) {
            for (var j = 0; j < 20; j++) {
                for (var k = 0; k < 30; k++) {
                    var res = CubicGraphBuilder.coordsToIndex(new int[]{i, j, k}, dims);
                    assertEquals(i * 20 * 30 + j * 30 + k, res);
                    var insertRes = used.add(res);
                    assertTrue(insertRes);
                }
            }
        }
    }

    @Test
    public void buildOneDimension() {
        int[] dims = {10};
        var graph = CubicGraphBuilder.cubic(dims);
        assertEquals(10, graph.size());
        assertEquals(Map.of(1, 1L), graph.get(0));
        for (var i = 1; i < 9; i++) {
            assertEquals(Map.of(i - 1, 1L, i + 1, 1L), graph.get(i));
        }
        assertEquals(Map.of(8, 1L), graph.get(9));
    }

    @Test
    public void buildTwoDimensions() {
        int[] dims = {10, 20};
        var graph = CubicGraphBuilder.cubic(dims);
        assertEquals(200, graph.size());
        for (var i = 0; i < 10; i++) {
            for (var j = 0; j < 20; j++) {
                var expected = new HashMap<Integer, Long>();
                if (i > 0) {
                    expected.put((i - 1) * 20 + j, 1L);
                }
                if (i + 1 < 10) {
                    expected.put((i + 1) * 20 + j, 1L);
                }
                if (j > 0) {
                    expected.put(i * 20 + j - 1, 1L);
                }
                if (j + 1 < 20) {
                    expected.put(i * 20 + j + 1, 1L);
                }
                assertEquals(expected, graph.get(i * 20 + j));
            }
        }
    }

    @Test
    public void buildThreeDimensions() {
        int[] dims = {10, 20, 30};
        var graph = CubicGraphBuilder.cubic(dims);
        assertEquals(6000, graph.size());
        for (var i = 0; i < 10; i++) {
            for (var j = 0; j < 20; j++) {
                for (var k = 0; k < 30; k++) {
                    var expected = new HashMap<Integer, Long>();
                    if (i > 0) {
                        expected.put((i - 1) * 600 + j * 30 + k, 1L);
                    }
                    if (i + 1 < 10) {
                        expected.put((i + 1) * 600 + j * 30 + k, 1L);
                    }
                    if (j > 0) {
                        expected.put(i * 600 + (j - 1) * 30 + k, 1L);
                    }
                    if (j + 1 < 20) {
                        expected.put(i * 600 + (j + 1) * 30 + k, 1L);
                    }
                    if (k > 0) {
                        expected.put(i * 600 + j * 30 + k - 1, 1L);
                    }
                    if (k + 1 < 30) {
                        expected.put(i * 600 + j * 30 + k + 1, 1L);
                    }
                    assertEquals(expected, graph.get(i * 600 + j * 30 + k));

                }
            }
        }
    }

}
