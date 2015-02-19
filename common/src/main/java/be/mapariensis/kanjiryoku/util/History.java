package be.mapariensis.kanjiryoku.util;

public interface History {
	public String back();

	public String forward();

	public void resetPointer();

	public void add(String command);

	public void clear();
}
