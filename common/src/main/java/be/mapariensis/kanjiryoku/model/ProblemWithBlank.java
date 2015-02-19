package be.mapariensis.kanjiryoku.model;

import java.util.List;

public abstract class ProblemWithBlank extends Problem {
	public final int blankIndex;
	public final Word blankWord;

	protected ProblemWithBlank(List<Word> words, int blankIndex) {
		super(words);
		this.blankIndex = blankIndex;
		this.blankWord = words.get(blankIndex);
	}

	public final boolean isBlank(Word w) {
		return blankWord.equals(w);
	}

}
