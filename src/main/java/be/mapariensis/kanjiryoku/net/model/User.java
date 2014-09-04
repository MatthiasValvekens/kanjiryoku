package be.mapariensis.kanjiryoku.net.model;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.exceptions.CommandQueueingException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.server.MessageHandler;
import be.mapariensis.kanjiryoku.net.server.Session;

public class User {
	private static final Logger log = LoggerFactory.getLogger(User.class);
	public final String handle;
	public final SocketChannel channel;
	protected final MessageHandler outbox;
	private Session session;
	private final Queue<ResponseHandler> activeResponseHandlers = new LinkedList<ResponseHandler>();
	private final Object sessionLock = new Object();
	public User(String handle, SocketChannel channel, MessageHandler outbox) {
		if(handle == null || outbox == null) throw new IllegalArgumentException();
		this.handle = handle;
		this.channel = channel;
		this.outbox = outbox;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((handle == null) ? 0 : handle.hashCode());
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
		User other = (User) obj;
		if (handle == null) {
			if (other.handle != null)
				return false;
		} else if (!handle.equals(other.handle))
			return false;
		return true;
	}
	public Session getSession() {
		synchronized(sessionLock) {
			return session;
		}
	}
	public Object sessionLock() {
		return sessionLock;
	}
	
	
	public void joinSession(Session sess) throws SessionException {
		synchronized(sessionLock) {
			if(session != null) throw new SessionException(String.format("User %s is in a session already", this.handle));
			else {
				session = sess;
			}
		}
	}
	public void leaveSession() {
		synchronized(sessionLock) {
			session = null;
		}
	}
	public boolean hasActiveResponseHandlers() {
		synchronized(activeResponseHandlers) {
			return !activeResponseHandlers.isEmpty();
		}
	}
	public void enqueueActiveResponseHandler(ResponseHandler hand) {
		synchronized(activeResponseHandlers) {
			activeResponseHandlers.add(hand);
		}
	}
	
	public void consumeActiveResponseHandler(NetworkMessage msg) throws ServerException {
		ResponseHandler rh;
		synchronized(activeResponseHandlers) {
			rh = activeResponseHandlers.poll();
			if(rh == null)
				throw new CommandQueueingException("No commands in queue.");
		}
		rh.handle(this, msg); // don't mind if this takes long, rh's should be queued anyway
	}
	
	public void purgeResponseHandlers() {
		synchronized(activeResponseHandlers) {
			activeResponseHandlers.clear();
		}
	}
	
	@Override
	public void finalize() {
		if(channel.isOpen()) {
			log.warn("User {} is being garbage collected, but channel is still open!", handle);
			try {
				NetworkMessage.signalProcessingError(channel,new ServerException("You have been GC'd, page the devs.", ServerException.ERROR_GENERIC));
				channel.close();
			} catch (Exception e) {}
		}
	}
	
	@Override
	public String toString() {
		try {
			return String.format("%s, %s",handle,channel.getRemoteAddress().toString());
		} catch (IOException e) {
			return String.format("%s, address could not be determined.",handle);
		} 
	}
	
}
