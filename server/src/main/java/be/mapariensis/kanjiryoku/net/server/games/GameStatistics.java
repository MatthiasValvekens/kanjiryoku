package be.mapariensis.kanjiryoku.net.server.games;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.json.JSONObject;

import be.mapariensis.kanjiryoku.net.model.User;

public class GameStatistics {
    private final User user;
    private final DateTime dt;

    private final Map<String, Score> categoryScores = new HashMap<>();

    public GameStatistics(User user, Map<String, Score> categoryScores) {
        this.user = user;
        this.categoryScores.putAll(categoryScores);
        this.dt = null;
    }

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
        ensure(cat).correct++;
    }

    public void fail(String cat) {
        ensure(cat).failed++;
    }

    private Score ensure(String cat) {
        Score sc = categoryScores.get(cat);
        if (sc == null) {
            categoryScores.put(cat, sc = new Score());
        }
        return sc;
    }

    public Map<String, Score> getScores() {
        return Collections.unmodifiableMap(categoryScores);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + categoryScores.hashCode();
        result = prime * result + ((dt == null) ? 0 : dt.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
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
        GameStatistics other = (GameStatistics) obj;
        if (!categoryScores.equals(other.categoryScores))
            return false;
        if (dt == null) {
            if (other.dt != null)
                return false;
        } else if (!dt.equals(other.dt))
            return false;
        if (user == null) {
            return other.user == null;
        } else return user.equals(other.user);
    }

}
