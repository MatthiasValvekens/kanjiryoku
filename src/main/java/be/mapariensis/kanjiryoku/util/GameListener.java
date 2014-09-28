package be.mapariensis.kanjiryoku.util;

import java.util.List;

import org.json.JSONObject;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.handlers.AnswerFeedbackHandler;

public interface GameListener {
	public void deliverProblem(Problem p, User to);
	public void deliverAnswer(User submitter, boolean wasCorrect, char resultChar, AnswerFeedbackHandler rh);
	public void problemSkipped(User submitter, AnswerFeedbackHandler rh);
	public void deliverStroke(User submitter, List<Dot> stroke);
	public void clearStrokes(User submitter); 
	public void finished(JSONObject statistics);
}
