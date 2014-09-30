package be.mapariensis.kanjiryoku.net.model;

import be.mapariensis.kanjiryoku.model.Problem;

public interface ProblemOrganizer {
	public boolean hasNext();
	public Problem next(boolean lastAnswer);
}
