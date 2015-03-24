package be.mapariensis.kanjiryoku.problemsets;

import be.mapariensis.kanjiryoku.model.Problem;

public interface ProblemOrganizer {
	public boolean hasNext();

	public Problem next(boolean lastAnswer);

	public String getCategoryName();
}
