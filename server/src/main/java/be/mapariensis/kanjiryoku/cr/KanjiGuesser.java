package be.mapariensis.kanjiryoku.cr;

import java.util.List;

public interface KanjiGuesser extends AutoCloseable {
    /**
     * This method should return a number of candidates given an (ordered) list
     * of strokes.
     *
     * @param width
     * 	Canvas width
     * @param height
     *  Canvas height
     * @param strokes
     *  strokes
     * @return A list of character guesses.
     */
    List<Character> guess(int width, int height, List<List<Dot>> strokes);

    boolean isOpen();
}
