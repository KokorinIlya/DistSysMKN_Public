package solution;

import internal.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

public class DijkstraProcessTest {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private void doStress(
            Function<Integer, Pair<Integer, Integer>> edgesFromToFun,
            Supplier<MessageBus> messageBusSupplier
    ) {
        for (int i = 0; i < 1000; i++) {
            if (i % 100 == 0) {
                log.info("Running iteration {}", i + 1);
            }
            var graph = RandomGraphBuilder.build(
                    20, 100,
                    edgesFromToFun,
                    0L, 100L,
                    true
            );
            var executor = new Executor(graph, messageBusSupplier);
            var start = new Random(System.nanoTime()).nextInt(graph.size());
            //noinspection ConstantConditions
            assert 0 <= start && start < graph.size();
            var expectedAns = SequentialDijkstra.heap(graph, start);
            var ans = executor.execute(start, 200);
            assertArrayEquals(expectedAns, ans);
        }
    }

    @Test
    public void stressRandomFair() {
        doStress((nc) -> new Pair<>(nc, nc * nc / 5), FairMessageBus::new);
    }

    @Test
    public void stressSparseFair() {
        doStress((nc) -> new Pair<>(0, nc * 2), FairMessageBus::new);
    }

    @Test
    public void stressDenseFair() {
        doStress((nc) -> new Pair<>(nc * nc / 10, nc * nc / 2), FairMessageBus::new);
    }

    @Test
    public void stressRandomFIFO() {
        doStress((nc) -> new Pair<>(nc, nc * nc / 5), FIFOMessageBus::new);
    }

    @Test
    public void stressSparseFIFO() {
        doStress((nc) -> new Pair<>(0, nc * 2), FIFOMessageBus::new);
    }

    @Test
    public void stressDenseFIFO() {
        doStress((nc) -> new Pair<>(nc * nc / 10, nc * nc / 2), FIFOMessageBus::new);
    }

    private void testCubic(int[] dims, Supplier<MessageBus> messageBusSupplier) {
        assert dims.length >= 1;
        var graph = CubicGraphBuilder.cubic(dims);

        for (var startIdx = 0; startIdx < graph.size(); startIdx++) {
            if (startIdx % 100 == 0) {
                log.info("Running iteration {}", startIdx + 1);
            }
            var executor = new Executor(graph, messageBusSupplier);
            var expectedAns = SequentialDijkstra.heap(graph, startIdx);
            var ans = executor.execute(startIdx, 200);
            assertArrayEquals(expectedAns, ans);
        }
    }

    @Test
    public void oneDimensionFair() {
        testCubic(new int[]{10}, FairMessageBus::new);
    }

    @Test
    public void twoDimensionsFair() {
        testCubic(new int[]{10, 20}, FairMessageBus::new);
    }

    @Test
    public void threeDimensionsFair() {
        testCubic(new int[]{10, 10, 10}, FairMessageBus::new);
    }

    @Test
    public void oneDimensionFIFO() {
        testCubic(new int[]{10}, FIFOMessageBus::new);
    }

    @Test
    public void twoDimensionsFIFO() {
        testCubic(new int[]{10, 20}, FIFOMessageBus::new);
    }

    @Test
    public void threeDimensionsFIFO() {
        testCubic(new int[]{10, 10, 10}, FIFOMessageBus::new);
    }
}
