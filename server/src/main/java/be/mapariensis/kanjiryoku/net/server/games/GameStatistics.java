package be.mapariensis.kanjiryoku.net.server.games;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.json.JSONObject;

import be.mapariensis.kanjiryoku.net.model.User;

public class GameStatistics {
	private final User user;
	private final DateTime dt;

	private static class Score {
		int correct, failed;
	}

	private final Map<String, Score> categoryScores = new HashMap<String, Score>();

	public GameStatistics(User user) {
		this.user = user;
		this.dt = DateTime.now();
	}

	public GameStatistics(User user, DateTime dt) {
		this.user = user;
		this.dt = dt;
	}

	public User getUser() {
		return user;
	}

	public DateTime getDateTime() {
		return dt;
	}

	public JSONObject toJSON() {
		JSONObject o = new JSONObject();
		for (Map.Entry<String, Score> cat : categoryScores.entrySet()) {
			JSONObject catKey = new JSONObject();
			catKey.put("correct", cat.getValue().correct);
			catKey.put("failed", cat.getValue().failed);
			o.put(cat.getKey(), catKey);
		}
		return o;
	}

	public void correct(String cat) {
		get(cat).correct++;
	}

	public void fail(String cat) {
		get(cat).failed++;
	}

	private Score get(String cat) {
		Score sc = categoryScores.get(cat);
		if (sc == null) {
			categoryScores.put(cat, sc = new Score());
		}
		return sc;
	}
}
