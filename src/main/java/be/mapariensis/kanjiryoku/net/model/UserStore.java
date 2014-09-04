package be.mapariensis.kanjiryoku.net.model;

import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.server.MessageHandler;

public final class UserStore {
	private final Map<SocketChannel, User> userConnMap = new ConcurrentHashMap<SocketChannel, User>();
	private final Map<String, User> userNameMap = new ConcurrentHashMap<String, User>();
	private final Object MODIFY_LOCK = new Object();
	public void addUser(User u) throws UserManagementException {
		synchronized(MODIFY_LOCK) {
			User other;
			if((other = userConnMap.get(u.channel))!=null) throw new UserManagementException(String.format("Connection already bound to user \"%s\"",other.handle));
			if(userNameMap.containsKey(u.handle)) throw new UserManagementException("User name taken.");
			userConnMap.put(u.channel, u);
			userNameMap.put(u.handle,u);
		}
	}
	public void removeUser(User u) {
		if(u==null) return;
		synchronized(MODIFY_LOCK) {
			userConnMap.remove(u.channel);
			userNameMap.remove(u.handle);
		}
	}
	
	public User verifyName(String name, SocketChannel peer){
		User peered = userConnMap.get(peer);
		User named = userNameMap.get(name);
		if(peered == null || named == null) return null;
		return peered.equals(named) ? peered : null;
	}
	
	public User requireUser(String name) throws UserManagementException {
		User u = getUser(name);
		if(u == null) throw new UserManagementException("No such user.");
		return u;
	}
	public User requireUser(SocketChannel peer) throws UserManagementException {
		User u = getUser(peer);
		if(u == null) throw new UserManagementException("No user bound to this peer.");
		return u;
	}
	
	public User getUser(String name) {
		return userNameMap.get(name);
	}
	public User getUser(SocketChannel peer) {
		return userConnMap.get(peer);
	}
	public MessageHandler getOutbox(SocketChannel channel) {
		User u = userConnMap.get(channel);
		return u==null ? null : u.outbox;
	}
	public MessageHandler requireOutbox(SocketChannel channel) throws UserManagementException {
		MessageHandler h = userConnMap.get(channel).outbox;
		if(h==null) throw new UserManagementException("No user bound to this peer.");
		else return h;
	}
	
	public Collection<User> getUsers() {
		return userConnMap.values();
	}
	
}
