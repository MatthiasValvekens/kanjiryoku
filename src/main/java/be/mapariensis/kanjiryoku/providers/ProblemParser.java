package be.mapariensis.kanjiryoku.providers;

import java.text.ParseException;

import be.mapariensis.kanjiryoku.model.Problem;

public interface ProblemParser<T extends Problem> {
	public T parseProblem(String input) throws ParseException;
}
