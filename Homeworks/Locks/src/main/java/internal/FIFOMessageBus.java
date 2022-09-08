package internal;

import java.util.*;

public class FIFOMessageBus implements MessageBus {
    private record ChannelId(int senderPid, int receiverPid) {
    }

    private final List<Queue<Message>> storage = new ArrayList<>();
    private final Map<ChannelId, Integer> index = new HashMap<>();
    private final Random random = new Random(System.nanoTime());

    @Override
    public void addNewMessage(int senderPid, int receiverPid, Object message) {
        assert index.size() == storage.size();
        assert senderPid > 0 && receiverPid > 0 && senderPid != receiverPid;
        var newMsg = new Message(senderPid, receiverPid, message);

        var chanId = new ChannelId(senderPid, receiverPid);
        var curIdx = index.getOrDefault(chanId, null);
        if (curIdx == null) {
            var newQueue = new ArrayDeque<Message>();
            newQueue.add(newMsg);
            storage.add(newQueue);
            var oldIdx = index.put(chanId, storage.size() - 1);
            assert oldIdx == null;
        } else {
            var curQueue = storage.get(curIdx);
            assert !curQueue.isEmpty();
            curQueue.add(newMsg);
        }
    }

    @Override
    public Message getNextMessage() {
        assert index.size() == storage.size();
        if (storage.size() == 0) {
            return null;
        }

        var randIdx = random.nextInt(storage.size());
        var curQueue = storage.get(randIdx);
        assert !curQueue.isEmpty();

        var result = curQueue.remove();
        if (curQueue.isEmpty()) {
            var chanIdToRemove = new ChannelId(result.sourcePid(), result.destinationPid());

            if (randIdx == storage.size() - 1) {
                storage.remove(storage.size() - 1);
                var removedIdx = index.remove(chanIdToRemove);
                assert Objects.equals(removedIdx, randIdx);
            } else {
                var lastQueue = storage.get(storage.size() - 1);
                assert !lastQueue.isEmpty();
                var lastMsg = lastQueue.peek();
                var lastChanId = new ChannelId(lastMsg.sourcePid(), lastMsg.destinationPid());
                assert Objects.equals(index.get(lastChanId), storage.size() - 1);

                storage.set(randIdx, lastQueue);
                var prevLastIdx = index.put(lastChanId, randIdx);
                assert Objects.equals(prevLastIdx, storage.size() - 1);

                var removedIx = index.remove(chanIdToRemove);
                assert Objects.equals(removedIx, randIdx);
                storage.remove(storage.size() - 1);
            }
        }
        return result;
    }
}
