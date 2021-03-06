package be.mapariensis.kanjiryoku.net.server.handlers;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.Principal;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.config.ServerConfig;
import be.mapariensis.kanjiryoku.net.exceptions.AuthenticationFailedException;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.model.IMessageHandler;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.SSLMessageHandler;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.model.UserData;
import be.mapariensis.kanjiryoku.net.secure.auth.AuthStatus;
import be.mapariensis.kanjiryoku.net.secure.auth.ServerAuthEngine;
import be.mapariensis.kanjiryoku.net.server.ConnectionContext;
import be.mapariensis.kanjiryoku.net.server.ServerCommand;
import be.mapariensis.kanjiryoku.net.server.SessionManager;
import be.mapariensis.kanjiryoku.net.server.UserManager;

public class CommandReceiverFactory {
    private static final Logger log = LoggerFactory
            .getLogger(CommandReceiverFactory.class);
    private final int usernameCharLimit;
    private final UserManager userman;
    private final Selector selector;
    private final SessionManager sessman;
    private final boolean requireAuth, sslAuthSufficient;

    public CommandReceiverFactory(ServerConfig config, UserManager userman,
            Selector selector, SessionManager sessman)
            throws BadConfigurationException {
        this.usernameCharLimit = config.getTyped(ConfigFields.USERNAME_LIMIT,
                Integer.class, ConfigFields.USERNAME_LIMIT_DEFAULT);
        this.requireAuth = config.getTyped(ConfigFields.REQUIRE_AUTH,
                Boolean.class, ConfigFields.REQUIRE_AUTH_DEFAULT);
        this.sslAuthSufficient = config.getTyped(
                ConfigFields.SSL_AUTH_SUFFICIENT, Boolean.class,
                ConfigFields.SSL_AUTH_SUFFICIENT_DEFAULT);
        this.userman = userman;
        this.selector = selector;
        this.sessman = sessman; 
    }

    public Runnable getReceiver(SocketChannel ch, NetworkMessage msg) {
        return new CommandReceiver(ch, msg);
    }

    private class CommandReceiver implements Runnable {
        final SocketChannel ch;
        final NetworkMessage msg;

        CommandReceiver(SocketChannel ch, NetworkMessage msg) {
            this.ch = ch;
            this.msg = msg;
        }

        @Override
        public void run() {
            try {
                final IMessageHandler h = ((ConnectionContext) ch.keyFor(
                        selector).attachment()).getMessageHandler();
                ServerCommand command;
                String commandString = msg.get(0).toUpperCase();
                try {
                    command = ServerCommand.valueOf(commandString);
                } catch (IllegalArgumentException ex) {
                    throw new ProtocolSyntaxException(String.format(
                            "Unknown command %s", commandString));
                }
                // check for REGISTER command (which gets special treatment)
                if (command == ServerCommand.REGISTER) {
                    handleRegister(msg, h);
                } else if (command == ServerCommand.AUTH) {
                    // schedule next auth step in separate thread.
                    userman.delegate(new AuthDelegate(h, this));
                } else {
                    User u = userman.getStore().getUser(ch);
                    if (u == null) {
                        if (command == ServerCommand.BYE) {
                            log.info(
                                    "Gracefully closing {} disconnected with BYE",
                                    ch);
                            h.close();
                        } else if (command != ServerCommand.HELLO)
                            throw new UserManagementException(
                                    "You must register before using any command other than HELLO, REGISTER or BYE");
                    } else
                        command.execute(msg, u, userman, sessman);
                }
            } catch (ServerException ex) {
                log.debug("Processing error", ex);
                queueProcessingError(ch, ex);
            } catch (IndexOutOfBoundsException ex) {
                log.debug("Badly formed command: {}", msg, ex);
                queueProcessingError(ch, new ProtocolSyntaxException(
                        "Badly formed command", ex));
            } catch (Exception e) {
                log.error("Failed to process command.", e);
                queueProcessingError(ch, new ServerBackendException(e));
            }
        }

        private void handleRegister(NetworkMessage msg, IMessageHandler h)
                throws ServerException, IOException {
            String handle = msg.get(1);
            handle = handle.substring(0,
                    Math.min(usernameCharLimit, handle.length())); // truncate
            if (requireAuth) {
                // shortcut if ssl authenticated
                if (sslAuthSufficient && h instanceof SSLMessageHandler) {
                    SSLEngine eng = ((SSLMessageHandler) h).getSSLEngine();
                    try {
                        Principal p = eng.getSession().getPeerPrincipal();
                        if (handle.equals(p.getName())) {
                            // SSL-authed users automatically get admin status,
                            // since they are trusted by the server.
                            // If required, you can disable non-SSL auth for
                            // admins altogether by simply not having any db
                            // records for admin users.
                            UserData ud = new UserData.Builder()
                                    .setUsername(handle).setAdmin(true)
                                    .deliver();
                            authenticate(handle, h, ud);
                        } else {
                            log.info(
                                    "Handle {} does not match principal name {}.",
                                    handle, p.getName());
                        }
                    } catch (SSLPeerUnverifiedException ex) {
                        // peer is not verified
                        log.debug("SSL peer not verified. Falling back on password-based auth.");
                        setUpAuthEngine(h);
                    }

                } else {
                    setUpAuthEngine(h);
                }
            } else {
                // authentication has been turned off, go ahead and
                // register.
                UserData ud = new UserData.Builder().setUsername(handle)
                        .deliver();
                authenticate(handle, h, ud);
            }
        }

        private void authenticate(String handle, IMessageHandler h, UserData ud)
                throws IOException, UserManagementException {
            SelectionKey key = ch.keyFor(selector);
            if (key == null) {
                log.error("Key cancelled before registration could complete! Aborting.");
                if (h != null)
                    h.close();
                return;
            }
            userman.register(new User(handle, ch, h, ud));
        }

        private void setUpAuthEngine(IMessageHandler h)
                throws ProtocolSyntaxException {
            ConnectionContext context = (ConnectionContext) ch.keyFor(selector)
                    .attachment();
            // if the auth engine is already set, REGISTER has been
            // called before. This should not be allowed.
            if (context.getAuthEngine() != null)
                throw new ProtocolSyntaxException();
            ServerAuthEngine eng = new ServerAuthEngine(
                    userman.getAuthBackend());
            // attach auth engine to connection context
            context.setAuthEngine(eng);
            userman.delegate(new AuthDelegate(h, this));
        }
    }

    private void queueProcessingError(SocketChannel ch, ServerException ex) {

        try {
            IMessageHandler h = ((ConnectionContext) ch.keyFor(selector)
                    .attachment()).getMessageHandler();
            h.send(ex.protocolMessage);
        } catch (CancelledKeyException | NullPointerException | IOException e) {
            log.warn("Failed to write message, peer already disconnected.");
        }
    }

    private class AuthDelegate implements Runnable {

        final IMessageHandler h;
        final CommandReceiver cr;

        public AuthDelegate(IMessageHandler h, CommandReceiver cr) {
            this.h = h;
            this.cr = cr;
        }

        @Override
        public void run() {
            ServerAuthEngine eng = ((ConnectionContext) cr.ch.keyFor(selector)
                    .attachment()).getAuthEngine();
            NetworkMessage reply;
            try {
                reply = eng.submit(cr.msg);
            } catch (AuthenticationFailedException e) {
                h.dispose(e.protocolMessage);
                return;
            } catch (ProtocolSyntaxException e) {
                log.debug("Protocol syntax exception in auth engine. Ignoring.");
                return;
            } catch (ServerBackendException e) {
                log.error("Error in server backend", e);
                h.dispose(e.protocolMessage);
                return;
            }
            if (eng.getStatus() == AuthStatus.SUCCESS) {
                try {
                    cr.authenticate(eng.getUsername(), h, eng.getUserData());
                } catch (UserManagementException e) {
                    h.dispose(e.protocolMessage);
                } catch (IOException | ServerBackendException e) {
                    log.warn("I/O error during user registration", e);
                    return;
                }
            }
            if (reply != null) {
                try {
                    h.send(reply);
                } catch (IOException e) {
                    log.warn("I/O error while communicating auth reply");
                    h.dispose();
                }
            }
        }
    }
}
