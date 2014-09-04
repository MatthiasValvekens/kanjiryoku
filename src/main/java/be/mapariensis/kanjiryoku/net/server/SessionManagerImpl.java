package be.mapariensis.kanjiryoku.net.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.model.User;

public class SessionManagerImpl implements SessionManager {
	private static final Logger log = LoggerFactory.getLogger(SessionManagerImpl.class);
	private final List<Session> sessions = new ArrayList<Session>();
	private final UserManager uman;
	private final Object LOCK = new Object();
	public SessionManagerImpl(UserManager uman) {
		this.uman = uman;
	}
	@Override
	public Session startSession(List<User> others) throws ServerException {
		synchronized(LOCK) {
			if(others.isEmpty()) throw new SessionException("Sessions must have at least one member");
			 Session res = new Session(this,freeSpot(), new HashSet<User>(others),others.get(0),uman);
			 sessions.set(res.getId(),res);
			 log.info("Established a session with id %05d, members are {} ",others);
			 return res;
		}
	}
	@Override
	public Session startSession(User master) throws ServerException {
		synchronized(LOCK) {
			HashSet<User> hs = new HashSet<User>();
			hs.add(master);
			 Session res = new Session(this,freeSpot(), hs,master,uman);
			 sessions.set(res.getId(),res);
			 log.info("Established a session with id {}, master is {}", res.getId(),master);
			 return res;
		}
	}
	
	private int freeSpot() {
		int i;
		for(i = 0;i<sessions.size();i++) {
			if(sessions.get(i)==null) return i;
		}
		sessions.add(null);
		return i;
	}

	@Override
	public void destroySession(Session sess) throws SessionException {
		synchronized(LOCK) {
			sess.purgeMembers();
			int id = sess.getId();
			sessions.set(id, null);
			sess.destroyed = true;
		}
	}

	@Override
	public User removeUser(User u) throws SessionException {
		synchronized(LOCK) {
			Session sess = u.getSession();
			if(sess == null) return null;
			User ret = sess.removeMember(u);
			if(ret == null) {
				log.info("Last user removed, destroying session.");
				int id = sess.getId();
				sessions.set(id, null);
			}
			return ret;
		}
	}



	@Override
	public Session getSession(int id) {
		synchronized(LOCK) {
			return sessions.get(id);
		}
	}

}
