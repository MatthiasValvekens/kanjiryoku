package be.mapariensis.jzinnia;

import java.util.LinkedList;
import java.util.List;

public class Features {
	private static final int kMaxCharacterSize = 50;
	private static class FeatureNode implements Comparable<FeatureNode> {
		final int index;
		final double value;
		public FeatureNode(int index, double value) {
			this.index = index;
			this.value = value;
		}
		@Override
		public int compareTo(FeatureNode o) {
			return Integer.compare(index, index);
		}
		
	}
	// wrapper for code readability
	private static class Node extends Pair<Double,Double> {
		public Node(Double x, Double y) {
			super(x, y);
		}
	};
	
	private final List<FeatureNode> featureNodes = new LinkedList<>(); // list of index-value pairs
	private static final Node MIDDLE = new Node(0.5,0.5);
	public void addFeature(int index, double value) {
		featureNodes.add(new FeatureNode(index,value));
	}
	
	public void makeBasicFeature(int offset, final Node first, final Node last) {
		// distance
		addFeature(offset + 1,10*distance(first,last));
		
		// degree
		addFeature(offset + 2,Math.atan2(last.y - first.y,last.x - first.x));
		
		// absolute position
		addFeature(offset + 3, 10 * (first.x - MIDDLE.x));
		addFeature(offset + 4, 10 * (first.y - MIDDLE.y));
		addFeature(offset + 5, 10 * (last.x - MIDDLE.x));
		addFeature(offset + 6, 10 * (last.y - MIDDLE.y));
		
		// absolute degree
		addFeature(offset + 7,Math.atan2(first.y - MIDDLE.y,first.x - MIDDLE.x));
		addFeature(offset + 8,Math.atan2(last.y - MIDDLE.y,last.x - MIDDLE.x));
		
		// absolute degree
		addFeature(offset + 9, 10 * distance(first,MIDDLE));
		addFeature(offset + 10, 10 * distance(last,MIDDLE));
		
		// diff
		addFeature(offset + 11, 5*(last.x - first.x));
		addFeature(offset + 12, 5*(last.y - first.y));
	}
	
	public void makeMoveFeature(int sid, final Node first, final Node last) {
		makeBasicFeature(100000 + sid * 1000,first,last);
	}
	
	public void makeVertexFeature(int sid, List<Pair<Node,Node>> nodePairs) {
		for(int i = 0; i<nodePairs.size() && i<=kMaxCharacterSize;i++) {
			Pair<Node,Node> pair = nodePairs.get(i);
			makeBasicFeature(sid * 1000 + 20 * i, pair.x, pair.y);
		}
	}
	
	public static double distanceSquared(Node p1, Node p2) {
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return dx*dx + dy*dy;
	}
	public static double distance(Node p1, Node p2) {
		return Math.sqrt(distanceSquared(p1, p2));
	}
	
	// return index of best node and minimal distance
	public static Pair<Integer,Double> minimumDistance(List<Node> nodes) {
		final int count = nodes.size();
		final Node first = nodes.get(0);
		final Node last = nodes.get(count-1);
		final double dx = last.x - first.x;
		final double dy = last.y - first.y;
		final double c = last.y * first.x - last.x * first.y;
		int ix = -1;
		double max = -1;
		for(int i = 0;i<nodes.size();i++) {
			Node n = nodes.get(i);
			double dist = Math.abs(dx*n.y-dy*n.x+c);
			if(dist > max) {
				max = dist;
				ix = i;
			}
		}
		
		return new Pair<>(ix,(max*max)/(dx*dx+dy*dy));
	}
	
}
