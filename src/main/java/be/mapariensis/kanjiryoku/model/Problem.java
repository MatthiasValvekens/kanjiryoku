package be.mapariensis.kanjiryoku.model;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;



public class Problem implements Iterable<Word>{
	private final List<Word> words;
	private final int blankIndex;
	public Problem(List<Word> words, int blankIndex) {
		this.words = Collections.unmodifiableList(words);
		this.blankIndex = blankIndex;
	}
	public Problem(List<Word> words, Word blank) {
		this(words,words.indexOf(blank));
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + blankIndex;
		result = prime * result + ((words == null) ? 0 : words.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Problem other = (Problem) obj;
		if (blankIndex != other.blankIndex)
			return false;
		if (words == null) {
			if (other.words != null)
				return false;
		} else if (!words.equals(other.words))
			return false;
		return true;
	}
	@Override
	public ListIterator<Word> iterator() {
		return words.listIterator();
	}
}
