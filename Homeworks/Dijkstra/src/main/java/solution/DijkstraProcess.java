package solution;

public interface DijkstraProcess {
    void onMessage(int senderPid, Object message);

    Long getDistance();

    void onComputationStart();
}
