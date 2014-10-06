package be.mapariensis.jzinnia;

import java.util.LinkedList;
import java.util.List;

public class ZCharacter {
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
	public int strokeLength(int ix) {
		return strokes.get(ix).size();
	}
	
	public Pair<Integer,Integer> at(int stroke, int point) {
		return strokes.get(stroke).get(point);
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
}
