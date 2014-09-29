package be.mapariensis.kanjiryoku.model;

import java.lang.Character.UnicodeBlock;
import java.util.List;

import be.mapariensis.kanjiryoku.util.Filter;
import be.mapariensis.kanjiryoku.util.UnicodeBlockFilter;

public final class KakiProblem extends ProblemWithBlank {

	public KakiProblem(List<Word> words, int blankIndex) {
		super(words, blankIndex);
	}
	@Override
	public String getFullSolution() {
		return blankWord.main;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Word w : this) {
			if(isBlank(w)) {
				sb.append('｛').append(w.furigana).append("｝（").append(w.main).append('）');
			} else if(w.furigana != null && !w.furigana.isEmpty()) {
				sb.append('［').append(w.furigana).append('］').append(w.main);
			} else sb.append(w.main);
		}
		return sb.toString();
	}
	@Override
	public Filter<Character> allowedChars() {
		return new UnicodeBlockFilter(UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
	}

}
