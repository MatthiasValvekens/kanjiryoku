package be.mapariensis.kanjiryoku.persistent.stats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import be.mapariensis.kanjiryoku.config.ServerConfig;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.games.GameStatistics;
import be.mapariensis.kanjiryoku.net.server.games.GameStatistics.Score;
import be.mapariensis.kanjiryoku.persistent.PostgresProvider;
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

	private static final String updateScores = "insert into kanji_gamestats (userid,game,category,correct,failed) values (?,?,?,?,?)";

	@Override
	public void updateScores(Game game, GameStatistics statistics)
			throws ServerBackendException {
		int type = getGameType(game);
		if (type == 0)
			throw new ServerBackendException(String.format("Unknown game: %s",
					game.toString()));
		try (Connection conn = ds.getConnection();
				PreparedStatement ps = conn.prepareStatement(updateScores)) {
			int uid = statistics.getUser().data.getId();
			int gameid = getGameType(game);
			conn.setAutoCommit(false);
			for (Map.Entry<String, Score> entry : statistics.entrySet()) {
				ps.setInt(1, uid);
				ps.setInt(2, gameid);
				ps.setString(3, entry.getKey());
				ps.setInt(4, entry.getValue().getCorrect());
				ps.setInt(5, entry.getValue().getFailed());
				ps.addBatch();
			}
			ps.executeBatch();
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			throw new ServerBackendException(e);
		}
	}

	private static final String aggregateScores = "select category, sum(correct) as correctTotal, sum(failed) as failedTotal"
			+ " from kanji_gamestats where userid=? and game=? group by category";

	@Override
	public GameStatistics aggregateScores(Game game, User user)
			throws ServerBackendException {
		int uid = user.data.getId();
		int gameid = getGameType(game);
		try (Connection conn = ds.getConnection();
				PreparedStatement ps = conn.prepareStatement(aggregateScores)) {
			ps.setInt(1, uid);
			ps.setInt(2, gameid);
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
			throw new ServerBackendException(e);
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
