package be.mapariensis.kanjiryoku.net.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.config.IProperties;
import be.mapariensis.kanjiryoku.cr.KanjiGuesserFactory;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.model.User;

public class SessionManagerImpl implements SessionManager {
	private static final Logger log = LoggerFactory.getLogger(SessionManagerImpl.class);
	private final List<Session> sessions = new ArrayList<Session>();
	private final UserManager uman;
	private final Object LOCK = new Object();
	private final IProperties config;
	private final KanjiGuesserFactory kgf;
	public SessionManagerImpl(IProperties config, UserManager uman, KanjiGuesserFactory kgf) {
		this.uman = uman;
		this.config = config;
		this.kgf = kgf;
	}
	@Override
	public Session startSession(User master, Game game) throws ServerException {
		IProperties gameSettings = config.getRequired(ConfigFields.GAME_SETTINGS_HEADER, IProperties.class).getRequired(game.name(), IProperties.class);
		GameServerInterface host;
		try {
			host = game.getServer(gameSettings, kgf.getGuesser(config.getRequired(ConfigFields.CR_SETTINGS_HEADER, IProperties.class)));
		} catch (IOException e) {
			throw new ServerBackendException(e);
		}
		synchronized(LOCK) {
			Session res = new Session(this,freeSpot(), master,uman,host);
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
	public void destroySession(Session sess) {
		if(sess.destroyed) return;
		try {
			sess.stopGame();
		} catch (ServerException e) {
			log.warn("Error while stopping game",e);
		}
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
			if(sess.getGame().running()) {
				destroySession(sess); // FIXME : Add support for leaving/joining running games
				return null;
			}
			User ret = sess.removeMember(u);
			if(ret == null) {
				log.info("Last user removed, destroying session.");
				int id = sess.getId();
				sessions.set(id, null);
				destroySession(sess);
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
