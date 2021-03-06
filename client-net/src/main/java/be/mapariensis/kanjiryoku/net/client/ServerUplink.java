package be.mapariensis.kanjiryoku.net.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;

import be.mapariensis.kanjiryoku.net.exceptions.ServerSubmissionException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.gui.UIBridge;
import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.client.handlers.WaitingResponseHandler;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.model.IMessageHandler;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.PlainMessageHandler;
import be.mapariensis.kanjiryoku.net.model.SSLMessageHandler;

public class ServerUplink extends Thread implements Closeable {
    private static final Logger log = LoggerFactory
            .getLogger(ServerUplink.class);
    private static final long SELECT_TIMEOUT = 500;
    private static final int PLAINTEXT_BUFSIZE = 8192;
    private static final int DELEGATE_TASK_WORKERS = 8;
    private final ExecutorService threadPool;
    private final ExecutorService delegatedTaskPool = Executors
            .newFixedThreadPool(DELEGATE_TASK_WORKERS);
    private final SocketChannel channel;
    private final SSLContext context;
    private volatile boolean keepOn = true;
    private boolean registerCompleted = false;
    private final Selector selector;
    private final InetAddress addr;
    private final int port;
    private String username;
    private IMessageHandler messageHandler;
    private SelectionKey key;
    private final UIBridge bridge;

    public ServerUplink(InetAddress addr, int port, String username,
            UIBridge bridge, SSLContext context) throws IOException {
        channel = SocketChannel.open();
        selector = Selector.open();
        this.addr = addr;
        this.port = port;
        this.bridge = bridge;
        this.username = username;
        threadPool = Executors.newSingleThreadExecutor();
        this.context = context;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private boolean modeFixed() {
        if (messageHandler instanceof SSLMessageHandler)
            return true;
        PlainMessageHandler pmh = (PlainMessageHandler) messageHandler;
        return pmh.getStatus() == PlainMessageHandler.Status.PERMANENT;
    }

    public void sslMode() throws ServerSubmissionException {
        if (!modeFixed()) {
            if (context == null) {
                bridge.getChat().displaySystemMessage(
                        "Server does not support plain text mode. Bail.");
                bridge.close();
            } else {
                bridge.getChat().displaySystemMessage("Mode accepted.");
                SSLEngine engine = context.createSSLEngine(addr.toString(),
                        port);
                engine.setUseClientMode(true);
                messageHandler = new SSLMessageHandler(key, engine,
                        delegatedTaskPool, PLAINTEXT_BUFSIZE);
                enqueueMessage(new NetworkMessage(ServerCommandList.REGISTER,
                        username));
            }
        }
    }

    public void plaintextMode() throws ServerSubmissionException {
        if (!modeFixed()) {
            if (context != null) {
                // this means we attempted to connect using SSL, but the server
                // refused.
                bridge.getChat().displaySystemMessage(
                        "Server does not support SSL mode. Bail.");
                bridge.close();
            } else {
                bridge.getChat().displaySystemMessage("Mode accepted.");
                enqueueMessage(new NetworkMessage(ServerCommandList.REGISTER,
                        username));
                ((PlainMessageHandler) messageHandler).setPermanent();
            }
        }
    }

    public void requestMode() {
        String mode = context == null ? Constants.MODE_PLAIN
                : Constants.MODE_TLS;
        bridge.getChat().displaySystemMessage(
                String.format("Requesting communication mode %s...", mode));
        try {
            messageHandler.send(new NetworkMessage(ServerCommandList.HELLO,
                    mode));
        } catch (IOException e) {
            log.error("I/O error while requesting mode.", e);
            bridge.close();
        }
    }

    @Override
    public void run() {
        // block until channel connects, and then switch to nonblocking mode
        try {
            bridge.getChat().displaySystemMessage("Connecting to server...");
            channel.configureBlocking(true);
            channel.connect(new InetSocketAddress(addr, port));
            channel.configureBlocking(false);
            key = channel.register(selector, SelectionKey.OP_READ);
            messageHandler = new PlainMessageHandler(key, PLAINTEXT_BUFSIZE);
            bridge.getChat().displaySystemMessage(
                    "Connected. Waiting for reply...");
        } catch (IOException e) {
            log.error("Failed to connect.", e);
            bridge.getChat().displaySystemMessage(
                    "Could not connect to server.");
            bridge.close();
            return;
        }
        while (keepOn) {
            int readyCount;
            try {
                readyCount = selector.select(SELECT_TIMEOUT);
            } catch (IOException e) {
                log.error("I/O error during selection", e);
                break;
            }
            if (!keepOn)
                break;
            if (readyCount == 0)
                continue;
            // clear event
            selector.selectedKeys().remove(key);

            if (!key.isValid()) {
                log.error("Key no longer valid");
                break; // TODO : handle this robustly
            }
            if (key.isReadable()) {
                final List<NetworkMessage> msgs;
                try {
                    msgs = messageHandler.readRaw();
                } catch (IOException ex) {
                    // FIXME : figure out a way to deal with forcefully closed connections,
                    //  and then downgrade this to EOFException
                    suppress(messageHandler);
                    try {
                        log.info("Peer {} shut down.",
                                channel.getRemoteAddress());
                        bridge.getChat()
                                .displaySystemMessage(
                                        "Peer connection lost. This is most likely a network or a server error.");
                        bridge.close();
                        break;
                    } catch (IOException e) {
                        break;
                    }
                }
                for (final NetworkMessage msg : msgs) {
                    // schedule command interpretation
                    if (!msg.isEmpty())
                        threadPool.execute(new CommandReceiver(msg));
                }
            }

            if (key.isWritable()) {
                try {
                    messageHandler.flushMessageQueue();
                } catch (SSLHandshakeException e) {
                    log.error("SSL Handshake failed.", e);
                    // inform user about nature of certificate error
                    Throwable rootCause = ExceptionUtils.getRootCause(e);
                    StringBuilder errorMsg = new StringBuilder(
                            "SSL handshake failed due to ");
                    if (rootCause instanceof CertificateException) {
                        errorMsg.append("certificate problem.\n");
                        errorMsg.append(rootCause.getMessage());
                    } else {
                        errorMsg.append("unknown issue.");
                    }
                    bridge.getChat().displaySystemMessage(errorMsg.toString());
                    bridge.close();
                } catch (IOException e) {
                    log.error("Failed to flush messages.", e);
                }
            }
        }
    }

    public boolean registered() {
        return registerCompleted;
    }

    public void flagRegisterComplete() {
        log.info("Registration complete");
        registerCompleted = true;
    }

    private class CommandReceiver implements Runnable {
        private final NetworkMessage msg;

        private CommandReceiver(NetworkMessage msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            if (msg.argCount() == 0)
                return;
            String cmdstring = msg.get(0);
            if (ClientServerException.isError(cmdstring)) {
                try {
                    bridge.getChat().displayErrorMessage(
                            ClientServerException.parseErrorCode(cmdstring),
                            msg.get(1));
                } catch (ParseException e) {
                    log.error("Unparsable server error code: {}.", cmdstring, e);
                    bridge.getChat().displayErrorMessage(
                            ClientServerException.ERROR_SERVER_COMM,
                            msg.get(1));
                }
                return;
            }
            ClientCommand command;
            try {
                command = ClientCommand.valueOf(cmdstring);
            } catch (IllegalArgumentException ex) {
                bridge.getChat().displayErrorMessage(
                        new ServerCommunicationException(ex));
                return;
            }
            try {
                command.execute(msg, bridge);
            } catch (ClientException e) {
                serverError(e);
            }
        }

    }

    private void serverError(ClientException ex) {
        bridge.getChat().displayErrorMessage(ex.errorCode, ex.getMessage());
        log.error("Error while parsing server message", ex);
    }

    public void enqueueMessage(NetworkMessage msg) throws ServerSubmissionException {
        if (messageHandler != null) {
            try {
                messageHandler.send(msg);
            } catch (IOException | CancelledKeyException e) {
                throw new ServerSubmissionException(msg, e);
            }
        }
    }

    public void enqueueMessage(NetworkMessage msg, ServerResponseHandler rh) throws ServerSubmissionException {
        activeResponseHandlers.add(rh);
        enqueueMessage(msg);
    }

    public static final int BLOCK_SLEEP_DELAY = 200;

    public NetworkMessage blockUntilResponse(NetworkMessage msg,
            WaitingResponseHandler wrh, long timeout) throws ServerSubmissionException {
        enqueueMessage(msg, wrh);
        NetworkMessage result;
        long startTime = System.currentTimeMillis();
        while ((result = wrh.getMessage()) == null) {
            if(timeout > 0 && (System.currentTimeMillis() - startTime) > timeout) {
                throw new ServerSubmissionException("Server response timed out");
            }
            try {
                Thread.sleep(BLOCK_SLEEP_DELAY);
            } catch (InterruptedException ignored) {
            }
        }
        return result;
    }

    @Override
    public void close() {
        log.info("Shutting down server uplink.");
        keepOn = false;
        suppress(messageHandler);
        suppress(selector);
        suppress(channel);
    }

    private static void suppress(Closeable c) {
        if (c == null)
            return;
        try {
            c.close();
        } catch (Exception e) {
            log.warn("Error while closing {}.", c, e);
        }
    }

    public String getUsername() {
        return username;
    }

    private final List<ServerResponseHandler> activeResponseHandlers = new LinkedList<>();

    public void consumeActiveResponseHandler(NetworkMessage msg)
            throws ClientException {
        int passedId;
        try {
            passedId = Integer.parseInt(msg.get(1));
        } catch (IndexOutOfBoundsException ex) {
            // the servercommand class should check this, but an extra safety
            // measure never hurts
            throw new ServerCommunicationException(
                    "Too few arguments for RESPOND");
        } catch (RuntimeException ex) {
            throw new ServerCommunicationException(ex);
        }
        if (passedId == -1) {
            bridge.getChat().getDefaultResponseHandler().handle(msg);
            return;
        }
        // there should only be a handful of active rh's at any one time, so
        // linear search is more than good enough
        synchronized (activeResponseHandlers) {
            for (ServerResponseHandler rh : activeResponseHandlers) {
                if (rh.id == passedId) {
                    rh.handle(msg); // don't mind if this takes long, rh's
                                    // should be queued anyway
                    activeResponseHandlers.remove(rh);
                    return;
                }
            }
        }
        throw new ClientException(String.format(
                "No response handler with id %s", passedId),
                ClientServerException.ERROR_QUEUE);
    }
}
