package internal;

import java.util.*;

public class SequentialDijkstra {
    public static Long[] array(List<Map<Integer, Long>> graph, int start) {
        assert 0 <= start && start < graph.size();
        var result = new Long[graph.size()];
        var used = new boolean[graph.size()];
        result[start] = 0L;

        for (int i = 0; i < graph.size(); i++) {
            int bestIndex = -1;

            for (int curIndex = 0; curIndex < graph.size(); curIndex++) {
                if (used[curIndex] || result[curIndex] == null) {
                    continue;
                }
                if (bestIndex == -1 || result[bestIndex] > result[curIndex]) {
                    assert bestIndex == -1 || result[bestIndex] != null;
                    bestIndex = curIndex;
                }
            }

            if (bestIndex == -1) {
                break;
            }

            assert result[bestIndex] != null && !used[bestIndex];
            used[bestIndex] = true;
            for (var entry : graph.get(bestIndex).entrySet()) {
                var neighbourIdx = entry.getKey();
                var w = entry.getValue();
                if (result[neighbourIdx] == null || result[bestIndex] + w < result[neighbourIdx]) {
                    assert !used[neighbourIdx];
                    result[neighbourIdx] = result[bestIndex] + w;
                }
            }
        }

        return result;
    }

    private static Pair<Integer, Long> getClosestNode(TreeMap<Long, Set<Integer>> nonVisited) {
        assert !nonVisited.isEmpty();
        var firstEntry = nonVisited.firstEntry();
        var minDist = firstEntry.getKey();
        assert minDist >= 0;
        var closestNodes = firstEntry.getValue();
        assert !closestNodes.isEmpty();
        var result = closestNodes.iterator().next();
        var removeRes = closestNodes.remove(result);
        assert removeRes;
        if (closestNodes.isEmpty()) {
            var setRemoveRes = nonVisited.remove(minDist);
            assert setRemoveRes == closestNodes;
        }
        return new Pair<>(result, minDist);
    }

    private static void removeNode(Map<Long, Set<Integer>> nonVisited, long dist, int nodeId) {
        assert dist >= 0;
        var sameDistSet = nonVisited.get(dist);
        assert sameDistSet != null;
        var removeRes = sameDistSet.remove(nodeId);
        assert removeRes;
        if (sameDistSet.isEmpty()) {
            var setRemoveRes = nonVisited.remove(dist);
            assert setRemoveRes == sameDistSet;
        }
    }

    private static void addNode(Map<Long, Set<Integer>> nonVisited, long dist, int nodeId) {
        assert dist >= 0;
        var sameDistSet = nonVisited.get(dist);
        if (sameDistSet == null) {
            var newSet = new HashSet<Integer>();
            newSet.add(nodeId);
            var putRes = nonVisited.put(dist, newSet);
            assert putRes == null;
        } else {
            assert !sameDistSet.contains(nodeId);
            var addRes = sameDistSet.add(nodeId);
            assert addRes;
        }
    }

    public static Long[] heap(List<Map<Integer, Long>> graph, int start) {
        assert 0 <= start && start < graph.size();
        var result = new Long[graph.size()];
        result[start] = 0L;
        var notVisited = new TreeMap<Long, Set<Integer>>();
        var startSet = new HashSet<Integer>();
        startSet.add(start);
        var putRes = notVisited.put(0L, startSet);
        assert putRes == null;

        while (!notVisited.isEmpty()) {
            var closest = getClosestNode(notVisited);
            var closestId = closest.first();
            var closestDist = closest.second();
            assert closestDist >= 0;
            assert Objects.equals(closestDist, result[closestId]);
            for (var entry : graph.get(closestId).entrySet()) {
                var neighbourId = entry.getKey();
                var w = entry.getValue();
                if (result[neighbourId] == null) {
                    result[neighbourId] = closestDist + w;
                    addNode(notVisited, result[neighbourId], neighbourId);
                } else if (result[neighbourId] > closestDist + w) {
                    removeNode(notVisited, result[neighbourId], neighbourId);
                    result[neighbourId] = closestDist + w;
                    addNode(notVisited, result[neighbourId], neighbourId);
                }
            }
        }

        return result;
    }
}
