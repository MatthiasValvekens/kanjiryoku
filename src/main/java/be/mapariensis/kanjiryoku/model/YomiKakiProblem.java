package be.mapariensis.kanjiryoku.model;

import java.util.List;

public class YomiKakiProblem extends ProblemWithBlank {
	public final boolean yomi;
	public YomiKakiProblem(List<Word> words, int blankIndex, boolean yomi) {
		super(words, blankIndex);
		this.yomi = yomi;
	}

}
