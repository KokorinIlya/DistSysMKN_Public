package internal;

import java.util.ArrayDeque;
import java.util.Queue;

public class FairMessageBus implements MessageBus {
    private final Queue<Message> queue = new ArrayDeque<>();

    @Override
    public void addNewMessage(int senderPid, int receiverPid, Object message) {
        queue.add(new Message(senderPid, receiverPid, message));
    }

    @Override
    public Message getNextMessage() {
        return queue.poll();
    }
}
