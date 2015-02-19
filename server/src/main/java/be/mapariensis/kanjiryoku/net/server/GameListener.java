package be.mapariensis.kanjiryoku.net.server;

import java.util.List;

import org.json.JSONObject;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.model.User;

public interface GameListener {
	public void deliverProblem(Problem p, User to);

	public void deliverAnswer(User submitter, boolean wasCorrect,
			char resultChar, ClientResponseHandler rh);

	public void problemSkipped(User submitter, boolean batonPass,
			ClientResponseHandler rh);

	public void deliverStroke(User submitter, List<Dot> stroke);

	public void clearStrokes(User submitter);

	public void finished(JSONObject statistics);
}
