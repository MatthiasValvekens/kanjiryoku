package be.mapariensis.kanjiryoku.cr;


import java.util.List;

public interface KanjiGuesser extends AutoCloseable {
	/**
	 * This method should return a number of candidates given an (ordered) list of strokes.
	 * @param strokes
	 * @param numCandidates
	 * @return
	 */
	public List<Character> guess(int width, int height, List<List<Dot>> strokes);
	
	@Override
	public void close();
}
