package be.mapariensis.kanjiryoku.model;

import java.util.List;

public final class YomiProblem extends ProblemWithBlank {
	public YomiProblem(List<Word> words, int blankIndex) {
		super(words, blankIndex);
	}

	@Override
	public String getFullSolution() {
		return blankWord.furigana;
	}

}
