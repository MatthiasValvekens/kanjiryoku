package be.mapariensis.kanjiryoku.net.model;

import be.mapariensis.kanjiryoku.net.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class MessageHandler implements IMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
    protected final SelectionKey key;
    private final CharsetDecoder decoder = Constants.ENCODING.newDecoder();
    protected volatile boolean disposed = false;

    protected MessageHandler(SelectionKey key) {
        if (key == null)
            throw new IllegalArgumentException();
        this.key = key;
    }

    protected abstract ByteBuffer getNetworkOutputBuffer();
    protected abstract ByteBuffer getApplicationInputBuffer();

    public List<NetworkMessage> readRaw() throws IOException {
        ByteBuffer appIn = getApplicationInputBuffer();
        // process unencrypted data
        // need to remember this for later
        int finalPosition = appIn.position();
        appIn.limit(finalPosition);
        // search backwards until we find the first EOM
        int lastEom;
        for (lastEom = appIn.limit() - 1; lastEom >= 0; lastEom--) {
            appIn.position(lastEom);
            if (appIn.get() == NetworkMessage.EOM)
                break;
        }
        // no full message received
        // the position is now at 0
        if (lastEom == -1) {
            appIn.compact();
            return Collections.emptyList();
        }
        // the position is one byte after the last EOM
        // so we can flip, and compact in the end
        appIn.flip();

        // decode the input into network messages
        CharBuffer decodedInput = CharBuffer.allocate(appIn.limit());
        // there should not be any incomplete characters,
        // after all, we cut off at the last EOM
        decoder.decode(appIn, decodedInput, true);
        decoder.flush(decodedInput);
        // prepare netIn buffer for next read
        appIn.limit(finalPosition);
        appIn.compact();

        decodedInput.flip();
        List<NetworkMessage> result = new ArrayList<>();

        while (decodedInput.position() < decodedInput.limit()) {
            // buildArgs stops when the limit is reached, or when it
            // reaches EOM
            result.add(NetworkMessage.buildArgs(decodedInput));
        }
        decoder.reset();
        return result;
    }

    protected final void flush() throws IOException {
        ByteBuffer netOut = getNetworkOutputBuffer();
        if (netOut.position() == 0) {
            log.trace("Nothing to flush");
            return;
        }
        SocketChannel ch = (SocketChannel) key.channel();
        log.trace("Will attempt to write {} bytes to {}. Writability {}.",
                netOut.position() - 1, ch.socket().getRemoteSocketAddress(),
                key.isWritable());
        if (!key.isWritable()) {
            synchronized (key) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
            return;
        }
        netOut.flip();
        ch.write(netOut);
        netOut.compact();
        if (netOut.position() > 0) {
            // short write
            log.trace("Short write! {} bytes left.", netOut.position());
            synchronized (key) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
        } else {
            synchronized (key) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            }
            if (disposed) {
                close();
            }
        }
    }
}
