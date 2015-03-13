package be.mapariensis.kanjiryoku.net.server;

import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.model.UserStore;

public interface UserManager {
	/**
	 * Register a user.
	 * 
	 * @throws UserManagementException
	 *             The user already exists, or could not be registered for some
	 *             other reason, such as a network error.
	 */
	public void register(User user) throws UserManagementException;

	/**
	 * Deregister a user and free all associated resources.
	 * 
	 * @param user
	 *            User
	 * @throws UserManagementException
	 *             User does not exist.
	 */
	public void deregister(User user) throws UserManagementException;

	/**
	 * Get the user associated with a given handle.
	 * 
	 * @param handle
	 *            A username.
	 * @throws UserManagementException
	 *             User does not exist.
	 */
	public User getUser(String handle) throws UserManagementException;

	/**
	 * Non-blockingly queue a message to send to a user. The string will be sent
	 * as soon as the monitor thread detects a writable socket.
	 * 
	 * @param user
	 * @param message
	 * @throws UserManagementException
	 */
	public void messageUser(User user, NetworkMessage message);

	public void messageUser(User user, NetworkMessage message,
			ClientResponseHandler handler);

	public void humanMessage(User user, String message);

	public void lobbyBroadcast(User user, NetworkMessage message);

	public void adminCommand(User issuer, int id, NetworkMessage commandMessage)
			throws UserManagementException, ProtocolSyntaxException;

	public UserStore getStore();

	public void delegate(Runnable run);
}
