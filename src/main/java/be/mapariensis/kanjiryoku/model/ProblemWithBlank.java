package be.mapariensis.kanjiryoku.model;

import java.util.List;

public class ProblemWithBlank extends Problem {
	public final int blankIndex;
	protected ProblemWithBlank(List<Word> words, int blankIndex) {
		super(words);
		this.blankIndex = blankIndex;
	}

}
