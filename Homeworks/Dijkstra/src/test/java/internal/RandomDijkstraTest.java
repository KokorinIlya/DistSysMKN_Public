package internal;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class RandomDijkstraTest {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private void doStress(Function<Integer, Pair<Integer, Integer>> edgesFromToFun) {
        for (int i = 0; i < 1000; i++) {
            if (i % 10 == 0) {
                log.info("Running iteration {}", i + 1);
            }
            var graph = RandomGraphBuilder.build(
                    20, 200, edgesFromToFun,
                    0L, 100L,
                    true
            );
            for (var start = 0; start < graph.size(); start++) {
                var ansArray = SequentialDijkstra.array(graph, start);
                var ansHeap = SequentialDijkstra.heap(graph, start);
                assertArrayEquals(ansArray, ansHeap);
            }
        }
    }

    @Test
    public void stressRandom() {
        doStress((nc) -> new Pair<>(nc, nc * nc / 5));
    }

    @Test
    public void stressSparse() {
        doStress((nc) -> new Pair<>(0, nc * 2));
    }

    @Test
    public void stressDense() {
        doStress((nc) -> new Pair<>(nc * nc / 10, nc * nc / 2));
    }
}
