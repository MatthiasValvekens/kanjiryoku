package be.mapariensis.kanjiryoku.providers;

import java.util.Collection;

import be.mapariensis.kanjiryoku.model.Problem;

public interface ProblemProvider<T extends Problem> {
	public Collection<T> getProblems();
}
