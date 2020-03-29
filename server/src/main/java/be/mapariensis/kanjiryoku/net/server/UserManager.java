package be.mapariensis.kanjiryoku.net.server;

import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.model.UserStore;
import be.mapariensis.kanjiryoku.net.secure.auth.AuthBackendProvider;

public interface UserManager {
    /**
     * Register a user.
     * 
     * @throws UserManagementException
     *             The user already exists, or could not be registered for some
     *             other reason, such as a network error.
     */
    void register(User user) throws UserManagementException;

    /**
     * Deregister a user and free all associated resources.
     * 
     * @param user
     *            User
     * @throws UserManagementException
     *             User does not exist.
     */
    void deregister(User user) throws UserManagementException;

    /**
     * Get the user associated with a given handle.
     * 
     * @param handle
     *            A username.
     * @throws UserManagementException
     *             User does not exist.
     */
    User getUser(String handle) throws UserManagementException;

    /**
     * Non-blockingly queue a message to send to a user. The string will be sent
     * as soon as the monitor thread detects a writable socket.
     * This method does not guarantee that the message will actually arrive.
     * 
     * @param user
     * 	Message recipient
     * @param message
     * 	Message content
     */
    void messageUser(User user, NetworkMessage message);

    void messageUser(User user, NetworkMessage message,
            ClientResponseHandler handler);

    void humanMessage(User user, String message);

    void lobbyBroadcast(User user, NetworkMessage message);

    void adminCommand(User issuer, NetworkMessage commandMessage)
            throws UserManagementException, ProtocolSyntaxException;

    AuthBackendProvider getAuthBackend();

    UserStore getStore();

    void delegate(Runnable run);
}
