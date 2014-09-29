package be.mapariensis.kanjiryoku.cr;


import java.util.List;

public interface KanjiGuesser {
	/**
	 * This method should return the {@code numCandidates} best candidates given an (ordered) list of strokes.
	 * @param strokes
	 * @param numCandidates
	 * @return
	 */
	public List<Character> guess(int width, int heigth, List<List<Dot>> strokes, int numCandidates);
}