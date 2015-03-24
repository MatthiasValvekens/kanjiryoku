package be.mapariensis.kanjiryoku.net.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.config.ServerConfig;
import be.mapariensis.kanjiryoku.cr.KanjiGuesserFactory;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.persistent.stats.ScoringBackend;
import be.mapariensis.kanjiryoku.util.IProperties;

public class SessionManagerImpl implements SessionManager {
	private static final Logger log = LoggerFactory
			.getLogger(SessionManagerImpl.class);
	private final List<Session> sessions = new ArrayList<Session>();
	private final UserManager uman;
	private final Object LOCK = new Object();
	private final ServerConfig config;
	private final ScoringBackend scorer;

	public SessionManagerImpl(ServerConfig config, UserManager uman,
			ScoringBackend scorer) {
		this.uman = uman;
		this.config = config;
		// For now, this gets passed as-is to any session created.
		// This will probably change when there's support for user-specified
		// session settings.
		this.scorer = scorer;
	}

	@Override
	public Session startSession(User master, Game game) throws ServerException,
			BadConfigurationException {
		IProperties gameSettings = config.getRequired(
				ConfigFields.GAME_SETTINGS_HEADER, IProperties.class)
				.getRequired(game.name(), IProperties.class);
		GameServerInterface host;
		KanjiGuesserFactory kgf = config.getKanjiGuesserFactory();
		try {
			host = game.getServer(gameSettings, kgf.getGuesser(config
					.getRequired(ConfigFields.CR_SETTINGS_HEADER,
							IProperties.class)), config.getProblemSetManager());
		} catch (IOException e) {
			throw new ServerBackendException(e);
		}
		synchronized (LOCK) {
			Session res = new Session(this, freeSpot(), master, uman, host,
					scorer);
			sessions.set(res.getId(), res);
			log.info("Established a session with id {}, master is {}",
					res.getId(), master);
			return res;
		}
	}

	private int freeSpot() {
		int i;
		for (i = 0; i < sessions.size(); i++) {
			if (sessions.get(i) == null)
				return i;
		}
		sessions.add(null);
		return i;
	}

	@Override
	public void destroySession(Session sess) {
		if (sess.destroyed)
			return;
		try {
			sess.stopGame();
		} catch (ServerException e) {
			log.warn("Error while stopping game", e);
		}
		synchronized (LOCK) {
			sess.purgeMembers();
			int id = sess.getId();
			sessions.set(id, null);
			sess.destroyed = true;
		}
	}

	@Override
	public User removeUser(User u) throws SessionException {
		synchronized (LOCK) {
			Session sess = u.getSession();
			if (sess == null)
				return null;
			if (sess.getGame().running()) {
				destroySession(sess); // FIXME : Add support for leaving/joining
										// running games
				return null;
			}
			User ret = sess.removeMember(u);
			if (ret == null) {
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
		synchronized (LOCK) {
			return sessions.get(id);
		}
	}

}
