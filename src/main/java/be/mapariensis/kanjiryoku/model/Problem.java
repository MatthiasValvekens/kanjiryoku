package be.mapariensis.kanjiryoku.model;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;



public class Problem implements Iterable<Word>{
	public final List<Word> words;
	protected Problem(List<Word> words) {
		this.words = Collections.unmodifiableList(words);
	}
	
	@Override
	public ListIterator<Word> iterator() {
		return words.listIterator();
	}
	
	
}
