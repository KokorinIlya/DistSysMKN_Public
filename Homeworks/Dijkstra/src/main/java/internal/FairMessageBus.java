package internal;

import java.util.ArrayDeque;
import java.util.Queue;

public class FairMessageBus implements MessageBus {
    private final Queue<Message> queue = new ArrayDeque<>();

    @Override
    public void addNewMessage(int senderPid, int receiverPid, Object message) {
        assert senderPid >= 0 && receiverPid >= 0 && senderPid != receiverPid;
        queue.add(new Message(senderPid, receiverPid, message));
    }

    @Override
    public Message getNextMessage() {
        return queue.poll();
    }
}
