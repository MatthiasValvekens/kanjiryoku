package be.mapariensis.kanjiryoku.net.model;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import be.mapariensis.kanjiryoku.net.commands.ClientCommandList;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.CommandQueueingException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.server.ClientResponseHandler;
import be.mapariensis.kanjiryoku.net.server.Session;

public class User {
	public final String handle;
	public final SocketChannel channel;
	public final UserData data;
	protected final IMessageHandler outbox;
	private Session session;
	private final List<ClientResponseHandler> activeResponseHandlers = new LinkedList<>();
	private final Object sessionLock = new Object();

	public User(String handle, SocketChannel channel, IMessageHandler outbox,
			UserData userData) {
		if (handle == null || outbox == null || userData == null)
			throw new IllegalArgumentException();
		this.handle = handle;
		this.channel = channel;
		this.outbox = outbox;
		this.data = userData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + handle.hashCode();
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
		return handle.equals(other.handle);
	}

	public Session getSession() {
		synchronized (sessionLock) {
			return session;
		}
	}

	public Object sessionLock() {
		return sessionLock;
	}

	public void joinSession(Session sess) throws SessionException {
		synchronized (sessionLock) {
			if (session != null)
				throw new SessionException(String.format(
						"User %s is in a session already", this.handle));
			else {
				session = sess;
			}
		}
	}

	public void leaveSession() {
		try {
			outbox.send(new NetworkMessage(ClientCommandList.RESETUI));
		} catch (CancelledKeyException | IOException ignored) {
		}
		synchronized (sessionLock) {
			session = null;
		}
	}

	public boolean hasActiveResponseHandlers() {
		synchronized (activeResponseHandlers) {
			return !activeResponseHandlers.isEmpty();
		}
	}

	public void enqueueActiveResponseHandler(ClientResponseHandler hand) {
		synchronized (activeResponseHandlers) {
			activeResponseHandlers.add(hand);

		}
	}

	public void consumeActiveResponseHandler(NetworkMessage msg)
			throws ServerException {
		int passedId;
		try {
			passedId = Integer.parseInt(msg.get(1));
		} catch (IndexOutOfBoundsException ex) {
			// the servercommand class should check this, but an extra safety
			// measure never hurts
			throw new ArgumentCountException(
					ArgumentCountException.Type.TOO_FEW,
					ServerCommandList.RESPOND);
		} catch (RuntimeException ex) {
			throw new ProtocolSyntaxException(ex);
		}
		if (passedId == -1)
			return;
		// there should only be a handful of active rh's at any one time, so
		// linear search is more than good enough
		synchronized (activeResponseHandlers) {
			for (ClientResponseHandler rh : activeResponseHandlers) {
				if (rh.id == passedId) {
					rh.handle(this, msg); // don't mind if this takes long, rh's
											// should be queued anyway
					activeResponseHandlers.remove(rh);
					return;
				}
			}
		}
		throw new CommandQueueingException(String.format(
				"No response handler with id %d", passedId));
	}

	public void purgeResponseHandlers() {
		synchronized (activeResponseHandlers) {
			activeResponseHandlers.clear();
		}
	}

	@Override
	public String toString() {
		try {
			return String.format("%s, %s", handle, channel.getRemoteAddress()
					.toString());
		} catch (IOException e) {
			return String
					.format("%s, address could not be determined.", handle);
		}
	}

	public boolean isConnectionSecure() {
		return outbox instanceof SSLMessageHandler;
	}
}
