package be.mapariensis.kanjiryoku.net.server;

import java.util.List;

import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.model.User;

public interface SessionManager {
	/**
	 * Register a session.
	 * @param users
	 *  The users of a session. The initiator goes first.
	 * @return
	 *    A Session object
	 * @throws SessionException
	 *    When a selected user is already in a session, or if an ID could not be obtained.
	 * @throws ServerException 
	 */
	public Session startSession(List<User> others) throws SessionException, ServerException;
	
	
	public void destroySession(Session sess) throws SessionException;
	
	/**
	 * Remove a user from its session. If the session master was removed and there are other users left in the pool, assign a new master.
	 * @param u
	 *   User to be removed.
	 * @param id
	 *   Session id. 
	 * @return
	 *   The new session master, or null if the last user was removed.
	 * @note When all users are removed from a session, the session manager
	 *   should destroy the session.
	 * @throws SessionException
	 *   The user to be removed is not a member.
	 */
	public User removeUser(User u) throws SessionException;
	
	
	public Session getSession(int id);


	public Session startSession(User master) throws ServerException;
}
