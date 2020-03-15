package be.mapariensis.kanjiryoku.net.server;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.commands.ClientCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.GameFlowException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.exceptions.UnsupportedGameException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.games.GameStatistics;
import be.mapariensis.kanjiryoku.persistent.PersistenceException;
import be.mapariensis.kanjiryoku.persistent.stats.ScoringBackend;

public class Session {
	private static final Logger log = LoggerFactory.getLogger(Session.class);
	private final int id;
	private final Set<User> users;
	private User master;
	private final SessionManager manager;
	volatile boolean destroyed;
	protected final Object LOCK = new Object();
	private final UserManager uman;
	private final GameServerInterface game;
	private final ScoringBackend scorer;

	public Session(SessionManager manager, int id, User master,
			UserManager uman, GameServerInterface game, ScoringBackend scorer)
			throws ServerException {
		if (game == null)
			throw new UnsupportedGameException("null");
		this.id = id;
		this.users = new HashSet<>();
		users.add(master);
		this.master = master;
		this.manager = manager;
		this.uman = uman;
		this.game = game;
		this.scorer = scorer;
		if (scorer == null) {
			log.debug("No scoring backend available. Scores will not be saved.");
		}
		synchronized (master.sessionLock()) {
			master.joinSession(this); // session is locked until we're done. No
										// threads can access our partially
										// constructed session
		}
	}

	public synchronized void start() throws GameFlowException,
			SessionException, ServerBackendException {
		checkDestroyed();
		game.startGame(this, users);
		broadcastHumanMessage(null, "Game started");
	}

	public void addMember(User u) throws SessionException {
		checkDestroyed();
		if (game.running())
			throw new SessionException("Not allowed while game is running");
		synchronized (LOCK) {
			if (u.getSession() == this)
				return;
			else
				u.joinSession(this);
			users.add(u);
			broadcastHumanMessage(u,
					String.format("User %s has joined the session.", u.handle));
			uman.humanMessage(u, "Joined session.");
		}
	}

	public synchronized void stopGame() throws ServerException {
		log.info("Stopping game...");
		checkDestroyed();
		game.close();
	}

	// package private because calling this method by itself violates the
	// contract between the session and the game server/users

	void purgeMembers() {
		broadcastHumanMessage(null, "Session end");
		synchronized (LOCK) {
			for (User u : new LinkedList<>(users)) {
				users.remove(u);
				if (u.getSession() != null)
					u.leaveSession();
			}
		}
	}

	User removeMember(User u) {
		synchronized (LOCK) {
			User previousMaster = master;
			users.remove(u);
			broadcastHumanMessage(u,
					String.format("User %s has left the session.", u));
			if (u == master) {
				// assign new leader
				master = users.isEmpty() ? null : users.iterator().next();

			}
			if (u.getSession() != null)
				u.leaveSession();
			if (master != null && master != previousMaster) {
				uman.humanMessage(master, "You are now the session master");
				log.info("Promoted {} to session master of session {}", master,
						id);
			}
			return master;
		}
	}

	public boolean isMaster(User u) {
		if (destroyed)
			return false;
		synchronized (LOCK) {
			return master.equals(u);
		}
	}

	public void kickUser(User caller, User kicked) throws SessionException {
		if (game.running())
			throw new SessionException("Not allowed while game is running");
		synchronized (LOCK) {
			if (kicked == caller || kicked == master || !isMember(kicked))
				throw new SessionException(
						"This user cannot be kicked from this session.");
			else if (caller != master)
				throw new SessionException(
						"You do not have sufficient privilege to kick users.");
			removeMember(kicked);
		}
		uman.humanMessage(kicked,
				String.format("You have been kicked from session %d", id));
		uman.humanMessage(caller, String.format(
				"User %s has been kicked from the session", kicked.handle));

	}

	public void broadcastMessage(User sender, NetworkMessage message) {
		if (destroyed)
			return;
		synchronized (LOCK) {
			for (User u : users) {
				if (!u.equals(sender))
					uman.messageUser(u, message);
			}
		}
	}

	public void broadcastMessage(User sender, NetworkMessage message,
			ClientResponseHandler rh) {
		if (destroyed)
			return;
		synchronized (LOCK) {
			for (User u : users) {
				if (!u.equals(sender))
					uman.messageUser(u, message, rh);
			}
		}
	}

	// sends a SAY type message to everyone except the sender

	public void broadcastHumanMessage(User sender, String message) {
		if (destroyed)
			return;
		synchronized (LOCK) {
			for (User u : users) {
				if (!u.equals(sender))
					uman.humanMessage(u, message);
			}
		}
	}

	/**
	 * Returns the ID of this session. Sessions monitored by the same session
	 * manager must never have the same ID.
	 */

	public final int getId() {
		return id;
	}

	public boolean isMember(User u) {
		if (destroyed)
			return false;
		synchronized (LOCK) {
			return users.contains(u);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((manager == null) ? 0 : manager.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Session other = (Session) obj;
		if (id != other.id)
			return false;
		if (manager == null) {
			return other.manager == null;
		} else return manager.equals(other.manager);
	}

	private void checkDestroyed() throws SessionException {
		if (destroyed)
			throw new SessionException("Session destroyed");
	}

	public void destroy() {
		manager.destroySession(this);
	}

	public GameServerInterface getGame() {
		return game;
	}

	public void statistics(List<GameStatistics> statistics) {
		JSONObject res = new JSONObject();
		for (GameStatistics data : statistics) {
			String uname = data.getUser().handle;
			res.put(uname, data.toJSON());
			if (scorer != null) {
				try {
					log.debug("Updating scores for user {}", uname);
					scorer.updateScores(game.getGame(), data);
				} catch (PersistenceException e) {
					log.warn("Failed to update scores for user {}.", uname, e);
				}
			}
		}
		broadcastMessage(null, new NetworkMessage(ClientCommandList.STATISTICS,
				res));
	}
}
