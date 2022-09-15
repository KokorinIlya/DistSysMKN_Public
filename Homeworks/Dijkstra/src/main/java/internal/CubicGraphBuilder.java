package internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CubicGraphBuilder {
    private static int[] calcElemsInDim(int[] dimensions) {
        assert dimensions.length > 0;
        var result = new int[dimensions.length];
        result[result.length - 1] = 1;
        for (int i = result.length - 1; i >= 1; i--) {
            result[i - 1] = result[i] * dimensions[i];
        }
        return result;
    }

    private static int coordsToIndex(int[] coords, int[] dimensions, int[] elemsInDim) {
        assert coords.length == elemsInDim.length && elemsInDim.length == dimensions.length && coords.length >= 1;
        var result = 0;
        for (var i = 0; i < coords.length; i++) {
            assert 0 <= coords[i] && coords[i] < dimensions[i];
            result += coords[i] * elemsInDim[i];
        }
        return result;
    }

    public static int coordsToIndex(int[] coords, int[] dimensions) {
        assert coords.length == dimensions.length && coords.length >= 1;
        var elemsInDim = calcElemsInDim(dimensions);
        return coordsToIndex(coords, dimensions, elemsInDim);
    }

    private static int calcNodesCount(int[] dimensions) {
        assert dimensions.length >= 1;
        int result = 1;
        for (var dim : dimensions) {
            assert dim > 0;
            result *= dim;
        }
        return result;
    }

    private static void doBuildCubic(
            List<Map<Integer, Long>> graph,
            int[] dimensions, int[] elemsInDim,
            int[] coords, int curCoordIndex
    ) {
        assert dimensions.length == coords.length && coords.length == elemsInDim.length && coords.length >= 1;
        assert 0 <= curCoordIndex && curCoordIndex <= coords.length;

        if (curCoordIndex == coords.length) {
            var curIdx = coordsToIndex(coords, dimensions, elemsInDim);

            for (var deltaIdx = 0; deltaIdx < dimensions.length; deltaIdx++) {
                if (coords[deltaIdx] + 1 < dimensions[deltaIdx]) {
                    coords[deltaIdx] += 1;
                    var neighbourIdx = coordsToIndex(coords, dimensions, elemsInDim);
                    var prevDist = graph.get(curIdx).put(neighbourIdx, 1L);
                    assert prevDist == null;
                    prevDist = graph.get(neighbourIdx).put(curIdx, 1L);
                    assert prevDist == null;
                    coords[deltaIdx] -= 1;
                }
            }
        } else {
            for (
                    coords[curCoordIndex] = 0;
                    coords[curCoordIndex] < dimensions[curCoordIndex];
                    coords[curCoordIndex]++
            ) {
                assert dimensions[curCoordIndex] > 0;
                doBuildCubic(graph, dimensions, elemsInDim, coords, curCoordIndex + 1);
            }
        }
    }

    public static List<Map<Integer, Long>> cubic(int[] dimensions) {
        var nodesCount = calcNodesCount(dimensions);
        var result = new ArrayList<Map<Integer, Long>>();
        for (var i = 0; i < nodesCount; i++) {
            result.add(new HashMap<>());
        }
        var coords = new int[dimensions.length];
        var elemsInDim = calcElemsInDim(dimensions);
        doBuildCubic(result, dimensions, elemsInDim, coords, 0);
        return result;
    }
}
