package solution;

import internal.Environment;

public class DijkstraProcessImpl implements DijkstraProcess {
    private final Environment env;

    public DijkstraProcessImpl(Environment env) {
        this.env = env;
    }

    @Override
    public void onMessage(int senderPid, Object message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Long getDistance() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void onComputationStart() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
