package internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomMessageBus implements MessageBus {
    private final List<Message> messages = new ArrayList<>();
    private final Random random = new Random(System.nanoTime());
    @Override
    public void addNewMessage(int senderPid, int receiverPid, Object message) {
        messages.add(new Message(senderPid, receiverPid, message));
    }

    @Override
    public Message getNextMessage() {
        if (messages.isEmpty()) {
            return null;
        } else {
            var index = random.nextInt(messages.size());
            var result = messages.get(index);
            messages.set(index, messages.get(messages.size() - 1));
            messages.remove(messages.size() - 1);
            return result;
        }
    }
}
