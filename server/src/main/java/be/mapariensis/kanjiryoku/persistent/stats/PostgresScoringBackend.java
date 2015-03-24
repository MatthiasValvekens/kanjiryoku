package be.mapariensis.kanjiryoku.persistent.stats;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import be.mapariensis.kanjiryoku.config.ServerConfig;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.games.GameStatistics;
import be.mapariensis.kanjiryoku.net.server.games.GameStatistics.Score;
import be.mapariensis.kanjiryoku.persistent.PersistenceException;
import be.mapariensis.kanjiryoku.persistent.PostgresProvider;
import be.mapariensis.kanjiryoku.persistent.util.NamedPreparedStatement;
import be.mapariensis.kanjiryoku.persistent.util.StatementIndexer;
import be.mapariensis.kanjiryoku.util.IProperties;

public class PostgresScoringBackend implements ScoringBackend {
	public static class Factory implements ScoringBackend.Factory {

		@Override
		public ScoringBackend setUp(ServerConfig serverConfig,
				IProperties scoringConfig) throws BadConfigurationException {
			DataSource ds = serverConfig.getDbConnection();
			if (ds == null) {
				// try postgresprovider

				if ((ds = (new PostgresProvider()).getDataSource(scoringConfig)) == null) {
					throw new BadConfigurationException(
							"No data source available");
				}
			}
			return new PostgresScoringBackend(ds);
		}

	}

	private final DataSource ds;

	public PostgresScoringBackend(DataSource ds) {
		this.ds = ds;
	}

	private static final String updateScoresSql = "insert into kanji_gamestats (userid,game,category,correct,failed) values (${userid},${game},${category},${correct},${failed})";
	private static final StatementIndexer updateScores = new StatementIndexer(
			updateScoresSql);

	@Override
	public void updateScores(Game game, GameStatistics statistics)
			throws PersistenceException {
		int type = getGameType(game);
		if (type == 0)
			throw new PersistenceException(String.format("Unknown game: %s",
					game.toString()));
		try (Connection conn = ds.getConnection();
				NamedPreparedStatement ps = updateScores.prepareStatement(conn)) {
			int uid = statistics.getUser().data.getId();
			int gameid = getGameType(game);
			conn.setAutoCommit(false);
			for (Map.Entry<String, Score> entry : statistics.entrySet()) {
				ps.setInt("userid", uid);
				ps.setInt("game", gameid);
				ps.setString("category", entry.getKey());
				ps.setInt("correct", entry.getValue().getCorrect());
				ps.setInt("failed", entry.getValue().getFailed());
				ps.addBatch();
			}
			ps.executeBatch();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	private static final String aggregateScoresSql = "select category, sum(correct) as correctTotal, sum(failed) as failedTotal"
			+ " from kanji_gamestats where userid=${userid} and game=${game} group by category";
	private static final StatementIndexer aggregateScores = new StatementIndexer(
			aggregateScoresSql);

	@Override
	public GameStatistics aggregateScores(Game game, User user)
			throws PersistenceException {
		int uid = user.data.getId();
		int gameid = getGameType(game);
		try (Connection conn = ds.getConnection();
				NamedPreparedStatement ps = aggregateScores
						.prepareStatement(conn)) {
			ps.setInt("userid", uid);
			ps.setInt("game", gameid);
			Map<String, Score> data = new HashMap<String, Score>();
			try (ResultSet res = ps.executeQuery()) {
				while (res.next()) {
					String cat = res.getString("category");
					Score sc = new Score(res.getInt("correctTotal"),
							res.getInt("failedTotal"));
					data.put(cat, sc);
				}
			}
			return new GameStatistics(user, data);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	private static int getGameType(Game game) {
		switch (game) {
		case TAKINGTURNS:
			return 1;
		default:
			return 0;
		}
	}
}
