package be.mapariensis.kanjiryoku.providers;

import java.text.ParseException;

import be.mapariensis.kanjiryoku.model.Problem;

public interface ProblemParser {
	public Problem parseProblem(String input) throws ParseException;
}
