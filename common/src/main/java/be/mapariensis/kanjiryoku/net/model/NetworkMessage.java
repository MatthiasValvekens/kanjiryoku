package be.mapariensis.kanjiryoku.net.model;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class NetworkMessage implements Iterable<String>,
        Comparable<NetworkMessage> {
    public static final char ATOMIZER = '"';
    public static final char DELIMITER = ' ';
    public static final char ESCAPE_CHAR = '\\';
    public static final byte EOM = (byte) 0x01;

    private final List<?> args;
    public final long timestamp;

    public NetworkMessage(Object obj, List<String> args) {
        ArrayList<Object> objs = new ArrayList<>();
        objs.add(obj);
        objs.addAll(args);
        this.args = objs;
        timestamp = System.currentTimeMillis();
    }

    public NetworkMessage(Object... args) {
        this(Arrays.asList(args));
    }

    public NetworkMessage(List<?> args) {
        this.args = args;
        timestamp = System.currentTimeMillis();
    }

    public String get(int ix) {
        return String.valueOf(args.get(ix));
    }

    public NetworkMessage truncate(int start) {
        return truncate(start, args.size());
    }

    public NetworkMessage truncate(int start, int end) {
        return new NetworkMessage(args.subList(start, end));
    }

    @Override
    public String toString() {
        return toString(0, args.size());
    }

    public String toString(int beginIndex) {
        return toString(beginIndex, args.size());
    }

    public String toString(int beginIndex, int endIndex) {
        if (argCount() == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        int i;
        for (i = beginIndex; i < endIndex - 1; i++) {
            sb.append(escapedAtom(get(i)));
            sb.append(DELIMITER);
        }
        sb.append(escapedAtom(get(i)));
        return sb.toString();
    }

    public static NetworkMessage buildArgs(String in) {
        return buildArgs(CharBuffer.wrap(in));
    }

    // TODO unit tests
    public static NetworkMessage buildArgs(CharBuffer in) {
        List<String> result = new ArrayList<>();
        boolean ignoreDelims = false, escape = false;
        StringBuilder sb = new StringBuilder(in.length());
        while (in.position() < in.limit()) {
            char cur = in.get();
            if (cur == (char) EOM) {
                break;
            } else if (cur == ESCAPE_CHAR) {
                if (escape)
                    sb.append(ESCAPE_CHAR);
                else
                    escape = true;
            } else {
                if (cur == ATOMIZER && !escape)
                    ignoreDelims = !ignoreDelims;
                else if (cur == DELIMITER && !ignoreDelims && sb.length() != 0
                        && !escape) { // check for delims
                    result.add(sb.toString());
                    sb.setLength(0); // clear buffer
                } else
                    sb.append(cur);
                escape = false;
            }
        }
        String last = sb.toString();
        if (!last.isEmpty())
            result.add(last.trim());
        return new NetworkMessage(result);
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<>() {
            Iterator<?> backend = args.iterator();

            @Override
            public boolean hasNext() {
                return backend.hasNext();
            }

            @Override
            public String next() {
                return String.valueOf(backend.next());
            }

            @Override
            public void remove() {
                backend.remove();
            }

        };
    }

    public int argCount() {
        return args.size();
    }

    public boolean isEmpty() {
        return args.isEmpty();
    }

    private static final String escapedAtomizer = String.valueOf(ESCAPE_CHAR) + ATOMIZER;

    public static String escapeSpecial(String string) { // does not escape
                                                        // delimiters
        return string.replace(String.valueOf(ESCAPE_CHAR), escapedEscape)
                .replace(String.valueOf(ATOMIZER), escapedAtomizer);
    }

    private static final String escapedEscape = String.valueOf(ESCAPE_CHAR) + ESCAPE_CHAR;

    private static String atomize(String string) {
        if (string.indexOf(DELIMITER) != -1) {
            return ATOMIZER + string + ATOMIZER;
        } else
            return string;
    }

    public static String escapedAtom(String string) {
        return atomize(escapeSpecial(string));
    }

    public NetworkMessage concatenate(Object... args) {
        return concatenate(Arrays.asList(args));
    }

    public NetworkMessage concatenate(List<?> args) {
        ArrayList<Object> newargs = new ArrayList<>(this.args);
        newargs.addAll(args);
        return new NetworkMessage(newargs);
    }

    @Override
    public int compareTo(NetworkMessage o) {
        if (o.timestamp == timestamp)
            return toString().compareTo(o.toString());
        long diff = timestamp - o.timestamp;
        return diff < 0 ? -1 : 1;
    }

}
