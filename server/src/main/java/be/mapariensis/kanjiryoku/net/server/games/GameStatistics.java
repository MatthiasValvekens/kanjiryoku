package be.mapariensis.kanjiryoku.net.server.games;

import org.joda.time.DateTime;
import org.json.JSONObject;

import be.mapariensis.kanjiryoku.net.model.User;

public class GameStatistics {
	private final User user;
	private final DateTime dt;
	int correct, skipped;

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
		o.put("Correct answers", correct);
		o.put("Failed problems", skipped);
		return o;
	}
}
