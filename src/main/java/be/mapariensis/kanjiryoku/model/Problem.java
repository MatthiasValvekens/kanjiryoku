package be.mapariensis.kanjiryoku.model;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;



public abstract class Problem implements Iterable<Word>{
	public final List<Word> words;
	protected Problem(List<Word> words) {
		this.words = Collections.unmodifiableList(words);
	}
	
	@Override
	public ListIterator<Word> iterator() {
		return words.listIterator();
	}
	/**
	 * Check whether the user's solution at the given position is correct.
	 * @param sol
	 *   The character list is a list of characters suggested by the guessing engine based on the user's input. 
	 * @return
	 *  A number between 0 and the return value of {@code userInputCount()}.
	 */
	// TODO document default implementation
	public final boolean checkSolution(List<Character> sol, int pos) {
		String s = getFullSolution();
		char relevantChar = s.charAt(pos);
		return sol.contains(relevantChar);
	}
	
	public abstract String getFullSolution();
}
