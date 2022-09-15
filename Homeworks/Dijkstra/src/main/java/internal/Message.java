package internal;

public record Message(int sourcePid, int destinationPid, Object content) {
}
