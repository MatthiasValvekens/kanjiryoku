package be.mapariensis.kanjiryoku.model;

import java.util.List;

public final class KakiProblem extends ProblemWithBlank {

	public KakiProblem(List<Word> words, int blankIndex) {
		super(words, blankIndex);
	}
	@Override
	public String getFullSolution() {
		return blankWord.main;
	}
}
