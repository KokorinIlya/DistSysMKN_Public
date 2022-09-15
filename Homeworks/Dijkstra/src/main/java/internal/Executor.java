package internal;

import solution.DijkstraProcess;
import solution.DijkstraProcessImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Executor {
    private final List<Map<Integer, Long>> graph;
    private final Supplier<MessageBus> messageBusSupplier;

    public Executor(List<Map<Integer, Long>> graph, Supplier<MessageBus> messageBusSupplier) {
        this.graph = graph;
        this.messageBusSupplier = messageBusSupplier;
    }

    public Long[] execute(int startId, int maxMessageSize) {
        assert 0 <= startId && startId < graph.size();
        var messageBus = this.messageBusSupplier.get();
        var systemState = new SystemState(maxMessageSize);

        var procs = new ArrayList<DijkstraProcess>();
        for (int i = 0; i < graph.size(); i++) {
            var env = new EnvironmentImpl(systemState, messageBus, i, graph);
            var proc = new DijkstraProcessImpl(env);
            procs.add(proc);
        }

        procs.get(startId).onComputationStart();

        while (true) {
            var msg = messageBus.getNextMessage();
            if (systemState.isExecutionFinished()) {
                if (msg != null) {
                    throw new IllegalStateException("Execution not finished, but in-flight messages exist");
                }
                var result = new Long[graph.size()];
                for (int i = 0; i < graph.size(); i++) {
                    result[i] = procs.get(i).getDistance();
                }
                return result;
            }
            if (msg == null) {
                throw new IllegalStateException("No messages to process, but execution not finished");
            }
            assert 0 <= msg.sourcePid() && msg.sourcePid() < graph.size() &&
                    0 <= msg.destinationPid() && msg.destinationPid() < graph.size() &&
                    msg.sourcePid() != msg.destinationPid();
            procs.get(msg.destinationPid()).onMessage(msg.sourcePid(), msg.content());
        }
    }
}

