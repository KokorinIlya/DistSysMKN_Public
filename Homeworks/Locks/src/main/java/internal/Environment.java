package internal;

public interface Environment {
    int getProcessId();
    int getNumberOfProcesses();
    void lock();
    void unlock();
    void send(int destinationPid, Object message);
}
