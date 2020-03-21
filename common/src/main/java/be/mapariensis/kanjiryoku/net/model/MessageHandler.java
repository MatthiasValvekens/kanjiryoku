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
        final int finalPosition = appIn.position();
        appIn.limit(finalPosition);
        // search backwards until we find the first EOM
        int lastEom;
        for (lastEom = finalPosition - 1; lastEom >= 0; lastEom--) {
            appIn.position(lastEom);
            if (appIn.get() == NetworkMessage.EOM)
                break;
        }
        // short read?
        // only re-register OP_READ when we're done with the data from the buffer
        final int bytesLeft = finalPosition - 1 - lastEom;

        final List<NetworkMessage> result;
        // This means that at least one full message was received
        //  (which could in principle have a length of 1, only the EOM)
        if(lastEom >= 0) {
            // the position is one byte after the last EOM
            // so we can flip, and compact in the end
            appIn.flip();

            // decode the input into network messages
            CharBuffer decodedInput = CharBuffer.allocate(appIn.limit());
            // there should not be any incomplete characters,
            // after all, we cut off at the last EOM
            decoder.decode(appIn, decodedInput, true);
            decoder.flush(decodedInput);
            // prepare appIn buffer for next read
            appIn.limit(finalPosition);

            decodedInput.flip();
            result = new ArrayList<>();

            while (decodedInput.position() < decodedInput.limit()) {
                // buildArgs stops when the limit is reached, or when it
                // reaches EOM
                result.add(NetworkMessage.buildArgs(decodedInput));
            }
            decoder.reset();
        } else {
            // no full message received
            // the position is now at 0, so the compact() call will
            // still do the right thing
            result = Collections.emptyList();
        }

        // compact buffer and re-register OP_READ as necessary
        appIn.compact();
        if (bytesLeft > 0) {
            log.trace("Short read! {} bytes left.", bytesLeft);
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
        }
        log.trace("Read message(s) {}", result);
        return result;
    }

    protected final int flush() throws IOException {
        ByteBuffer netOut = getNetworkOutputBuffer();
        if (netOut.position() == 0) {
            log.trace("Nothing to flush");
            return 0;
        }
        SocketChannel ch = (SocketChannel) key.channel();
        log.trace("Will attempt to write {} bytes to {}. Writability {}.",
                netOut.position() - 1, ch.socket().getRemoteSocketAddress(),
                key.isWritable());
        if (!key.isWritable()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            return 0;
        }
        netOut.flip();
        int bytesWritten = ch.write(netOut);
        netOut.compact();
        if (netOut.position() > 0) {
            // short write
            log.trace("Short write! {} bytes left.", netOut.position());
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } else {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            if (disposed) {
                close();
            }
        }
        return bytesWritten;
    }
}
