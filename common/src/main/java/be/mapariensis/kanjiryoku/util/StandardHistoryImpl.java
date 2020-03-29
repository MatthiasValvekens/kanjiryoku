package be.mapariensis.kanjiryoku.util;

import java.util.ArrayDeque;
import java.util.Deque;

public class StandardHistoryImpl implements History {
    private final Deque<String> contents;
    private final Deque<String> rest;
    private final Object LOCK = new Object();
    private final int size;
    private int entryCount = 0;

    public StandardHistoryImpl(int size) {
        this.contents = new ArrayDeque<String>(size);
        this.rest = new ArrayDeque<String>(size);
        this.size = size;
    }

    @Override
    public String back() {
        synchronized (LOCK) {
            if (contents.isEmpty()) {
                return rest.isEmpty() ? "" : rest.peek(); // stay on the last
                                                            // item
            } else {
                String thing = contents.pop();
                rest.push(thing);
                return thing;
            }
        }
    }

    @Override
    public String forward() {
        synchronized (LOCK) {
            if (rest.isEmpty()) {
                return "";
            } else {
                String thing = rest.pop();
                contents.push(thing);
                return rest.isEmpty() ? "" : rest.peek();
            }
        }
    }

    @Override
    public void resetPointer() {
        synchronized (LOCK) {
            while (!rest.isEmpty()) {
                contents.push(rest.pop());
            }
        }
    }

    @Override
    public void add(String command) {
        synchronized (LOCK) {
            resetPointer();
            if (!contents.isEmpty() && contents.peek().equals(command))
                return;
            if (entryCount == size) {
                contents.pollLast(); // remove element from tail
            } else {
                entryCount++;
            }
            contents.push(command);
        }
    }

    @Override
    public void clear() {
        synchronized (LOCK) {
            contents.clear();
            rest.clear();
        }

    }

}
