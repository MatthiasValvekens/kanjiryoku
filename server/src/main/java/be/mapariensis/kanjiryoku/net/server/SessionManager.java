package be.mapariensis.kanjiryoku.net.server;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.model.User;

/**
 * An interface for managing sessions. NOTE: it is imperative that
 * implementations of this interface not create their own worker threads,
 * because they will almost certainly deadlock when trying to access a user's
 * session information.
 * 
 * @author Matthias Valvekens
 * @version 1.0
 */
public interface SessionManager {

	void destroySession(Session sess);

	/**
	 * Remove a user from its session. If the session master was removed and
	 * there are other users left in the pool, assign a new master.
	 * 
	 * @param u
	 *            User to be removed.
	 * @return The new session master, or null if the last user was removed.
	 * @implSpec When all users are removed from a session, the session manager
	 *       should destroy the session.
	 * @throws SessionException
	 *             The user to be removed is not a member.
	 */
	User removeUser(User u) throws SessionException;

	Session getSession(int id);

	/**
	 * Register a session with the given session leader.
	 * 
	 * @param master
	 *        The session master
	 * @param game
	 * 		  The game to be played
	 * @return A Session object
	 * @throws SessionException
	 *             When a selected user is already in a session, or if an ID
	 *             could not be obtained.
	 * @throws ServerException
	 * 	Error in server execution flow.
	 * @throws BadConfigurationException
     * 	Problem reading game settings from configuration.
	 */

	Session startSession(User master, Game game) throws ServerException,
			BadConfigurationException;
}
