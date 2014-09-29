package be.mapariensis.kanjiryoku.util;

public interface Filter<T> {
	public boolean accepts(T thing);
}
