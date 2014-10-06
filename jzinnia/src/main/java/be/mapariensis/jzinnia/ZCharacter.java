package be.mapariensis.jzinnia;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class ZCharacter implements Iterable<List<Pair<Integer,Integer>>> {
	private final int width, height;
	private final List<List<Pair<Integer,Integer>>> strokes = new LinkedList<List<Pair<Integer,Integer>>>();
	public ZCharacter(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public void addStroke(List<Pair<Integer,Integer>> stroke) {
		strokes.add(stroke);
	}
	
	public int strokeCount() {
		return strokes.size();
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}

	@Override
	public Iterator<List<Pair<Integer, Integer>>> iterator() {
		return strokes.iterator();
	}
	
	public ListIterator<List<Pair<Integer,Integer>>> listIterator() {
		return strokes.listIterator();
	}
}
