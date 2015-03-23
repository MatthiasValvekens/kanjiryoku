package be.mapariensis.kanjiryoku.persistent.stats;

import be.mapariensis.kanjiryoku.config.ServerConfig;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.games.GameStatistics;
import be.mapariensis.kanjiryoku.persistent.PersistenceException;
import be.mapariensis.kanjiryoku.util.IProperties;

public interface ScoringBackend {

	public interface Factory {
		public ScoringBackend setUp(ServerConfig serverConfig,
				IProperties scoringConfig) throws BadConfigurationException;
	}

	public void updateScores(Game game, GameStatistics statistics)
			throws PersistenceException;

	public GameStatistics aggregateScores(Game game, User user)
			throws PersistenceException;
}
