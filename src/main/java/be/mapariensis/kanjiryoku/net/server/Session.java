package be.mapariensis.kanjiryoku.net.server;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.exceptions.GameFlowException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.exceptions.UnsupportedGameException;
import be.mapariensis.kanjiryoku.net.model.ClientCommand;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.util.GameListener;

public class Session {
	private static final Logger log = LoggerFactory.getLogger(Session.class);
	private final int id;
	private final Set<User> users;
	private User master;
	private final SessionManager manager;
	volatile boolean destroyed;
	protected final Object LOCK = new Object();
	private final UserManager uman;
	public final GameServerInterface game;
	
	public Session(SessionManager manager, int id, User master, UserManager uman, GameServerInterface game) throws ServerException {
		if(game == null) throw new UnsupportedGameException("null");
		this.id = id;
		this.users = new HashSet<User>();
		users.add(master);
		this.master = master;
		this.manager = manager;
		this.uman = uman;
		this.game = game;
		this.game.addProblemListener(new NetworkedGameListener());
		synchronized(master.sessionLock()) {
			master.joinSession(this); // session is locked until we're done. No threads can access our partially constructed session
		}
	}
	public synchronized void start() throws GameFlowException, SessionException {
		checkDestroyed();
		game.startGame(users);
		broadcastHumanMessage(null, "Game started");
	}
	public void addMember(User u) throws SessionException {
		checkDestroyed();
		if(game.running()) throw new SessionException("Not allowed while game is running");
		synchronized(LOCK) {
			if(u.getSession() == this) return;
			users.add(u);
			if(u.getSession() != this) u.joinSession(this);
			broadcastHumanMessage(u, String.format("User %s has joined the session.",u));
			uman.humanMessage(u, "Joined session.");
			return;
		}
	}
	public synchronized void stopGame() throws ServerException {
		log.info("Stopping game...");
		checkDestroyed();
		game.close();
	}
	
	//package private because calling this method by itself violates the contract between the session and the game server/users
	
	void purgeMembers() {
		broadcastHumanMessage(null, "Received session kill signal");
		synchronized(LOCK) {
			for(User u : new LinkedList<User>(users)) {
				users.remove(u);
				if(u.getSession() != null) u.leaveSession();
			}
		}
	}
	User removeMember(User u) {
		synchronized(LOCK) {
			User previousMaster = master;
			users.remove(u);
			broadcastHumanMessage(u, String.format("User %s has left the session.",u));
			if(u == master) {
				//assign new leader
				master =  users.isEmpty() ? null : users.iterator().next();
				
			}
			if(u.getSession() != null) u.leaveSession();
			if(master != null && master != previousMaster) {
				uman.humanMessage(master, "You are now the session master");
				log.info("Promoted {} to session master of session {}",master,id);
			}
			return master;
		}
	}	
	
	
	public boolean isMaster(User u) {
		if(destroyed) return false;
		synchronized(LOCK) {
			return master.equals(u);
		}
	}
	
	public void kickUser(User caller, User kicked) throws SessionException {
		if(game.running()) throw new SessionException("Not allowed while game is running");
		synchronized(LOCK) {
			if(kicked == caller || kicked == master || !isMember(kicked))
				throw new SessionException("This user cannot be kicked from this session.");
			else if(caller != master)
				throw new SessionException("You do not have sufficient privilege to kick users.");
			removeMember(kicked);
		}
		uman.humanMessage(kicked, String.format("You have been kicked from session %d",id));
		uman.humanMessage(caller, String.format("User %s has been kicked from the session",kicked.handle));
		
	}
	public void broadcastMessage(User sender, String message) { // pass null for server message
		if(destroyed) return;
		synchronized(LOCK) {
			for(User u : users) {
				if(u.equals(sender)) continue;
				uman.messageUser(u, message);
			}
		}
	}
	
	public void broadcastMessage(User sender, NetworkMessage message) {
		if(destroyed) return;
		synchronized(LOCK) {
			for(User u : users) {
				if(u.equals(sender)) continue;
				uman.messageUser(u, message);
			}
		}
	}
	// sends a SAY type message to everyone except the sender
	public void broadcastHumanMessage(User sender, String message) {
		if(destroyed) return;
		synchronized(LOCK) {
			for(User u : users) {
				if(u.equals(sender)) continue;
				uman.humanMessage(u, message);
			}
		}
	}

	/**
	 * Returns the ID of this session. Sessions monitored by the same session manager must never have the same ID.
	 */
	public final int getId() {
		return id;
	}
	
	public boolean isMember(User u) {
		if(destroyed) return false;
		synchronized(LOCK) {
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
			if (other.manager != null)
				return false;
		} else if (!manager.equals(other.manager))
			return false;
		return true;
	}
	
	public boolean isDestroyed() {
		return destroyed;
	}
	private void checkDestroyed() throws SessionException {
		if(destroyed) throw new SessionException("Session destroyed");
	}
	
	private class NetworkedGameListener implements GameListener {

		@Override
		public void deliverProblem(Problem p, User to) {
			broadcastMessage(null,new NetworkMessage(ClientCommand.PROBLEM,to.handle,p.toString()));
		}

		@Override
		public void deliverAnswer(User submitter, boolean wasCorrect) {
			broadcastMessage(null,new NetworkMessage(ClientCommand.ANSWER,submitter.handle,wasCorrect));
		}

		@Override
		public void deliverStroke(User submitter, List<Dot> stroke) {
			broadcastMessage(null,new NetworkMessage(ClientCommand.STROKE,submitter.handle,stroke));
		}

		@Override
		public void clearStrokes() {
			broadcastMessage(null, ClientCommand.CLEARSTROKES.toString());
		}

		@Override
		public void finished() {
			manager.destroySession(Session.this);
		}		
	}
}
