package be.mapariensis.kanjiryoku.util;

import java.util.List;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.model.User;

public interface GameListener {
	public void deliverProblem(Problem p, User to);
	public void deliverAnswer(User submitter, boolean wasCorrect, char resultChar);
	public void deliverStroke(User submitter, List<Dot> stroke);
	public void clearStrokes(User submitter); 
	public void finished();
}
