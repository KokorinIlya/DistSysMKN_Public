package internal;

import java.util.*;
import java.util.function.Function;

public class RandomGraphBuilder {
    public static List<Map<Integer, Long>> build(
            int nodesFrom, int nodesTo,
            Function<Integer, Pair<Integer, Integer>> edgesFromToFun,
            long weightsFrom, long weightsTo,
            boolean skipEdges
    ) {
        assert 0 < nodesFrom && nodesFrom < nodesTo;
        assert 0 <= weightsFrom && weightsFrom < weightsTo;

        var random = new Random(System.nanoTime());
        var nodesCount = random.nextInt(nodesFrom, nodesTo);
        var edgesFromTo = edgesFromToFun.apply(nodesCount);
        var edgesFrom = edgesFromTo.first();
        var edgesTo = edgesFromTo.second();
        assert 0 <= edgesFrom && edgesFrom < edgesTo;
        var edgesCount = random.nextInt(edgesFrom, edgesTo);

        var edgesAdded = 0;
        var result = new ArrayList<Map<Integer, Long>>();
        for (var i = 0; i < nodesCount; i++) {
            result.add(new HashMap<>());
        }

        while (edgesAdded < edgesCount) {
            var source = random.nextInt(nodesCount);
            var destination = random.nextInt(nodesCount);
            //noinspection ConstantConditions
            assert 0 <= source && source < nodesCount && 0 <= destination && destination < nodesCount;
            var weight = random.nextLong(weightsFrom, weightsTo);
            if (result.get(source).containsKey(destination)) {
                if (skipEdges) {
                    edgesAdded++;
                }
            } else {
                var addRes = result.get(source).put(destination, weight);
                assert addRes == null;
            }
        }

        return result;
    }
}
