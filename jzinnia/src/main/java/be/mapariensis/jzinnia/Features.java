package be.mapariensis.jzinnia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Features {
	private static final int kMaxCharacterSize = 50;
	public static class FeatureNode implements Comparable<FeatureNode> {
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
	public static class Node extends Pair<Double,Double> {
		public Node(Double x, Double y) {
			super(x, y);
		}
	};
	
	private final List<FeatureNode> featureNodes = new ArrayList<>(); // list of index-value pairs
	private static final Node MIDDLE = new Node(0.5,0.5);
	public void addFeature(int index, double value) {
		featureNodes.add(new FeatureNode(index,value));
	}
	
	private void makeBasicFeature(int offset, final Node first, final Node last) {
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
	
	private void makeMoveFeature(int sid, final Node first, final Node last) {
		makeBasicFeature(100000 + sid * 1000,first,last);
	}
	
	private void makeVertexFeature(int sid, List<Pair<Integer,Pair<Node,Node>>> nodePairs) {
		for(Pair<Integer,Pair<Node,Node>> pair : nodePairs) {
			int id = pair.x;
			System.out.println(id);
			if(id>kMaxCharacterSize) continue;
			makeBasicFeature(sid * 1000 + 20 * id, pair.y.x, pair.y.y);
		}
	}
	
	public static final double TRESHOLD = 0.001;
	private void getVertex(List<Node> nodes,int firstIx,int lastIx,int id, List<Pair<Integer,Pair<Node,Node>>> nodePairs) {
		{
			Node first = nodes.get(firstIx);
			Node last = nodes.get(lastIx);
			Pair<Node,Node> curPair = new Pair<>(first,last);
			nodePairs.add(new Pair<>(id,curPair));
		}
		Pair<Integer,Double> distPair = minimumDistance(nodes.subList(firstIx, lastIx+1));
		if(distPair.y > TRESHOLD) {
			getVertex(nodes, firstIx, distPair.x, id * 2 + 1, nodePairs);
			getVertex(nodes, distPair.x,lastIx, id * 2 + 2, nodePairs);
		}
	}
	public void read(ZCharacter character) {
		featureNodes.clear();
		// bias term
		
		featureNodes.add(new FeatureNode(0, 1.0));
		
		List<List<Node>> nodes = new ArrayList<List<Node>>(character.strokeCount());
		{
			double height = character.getHeight();
			double width = character.getWidth();
			if(height == 0 || width == 0 || character.strokeCount() == 0) throw new IllegalArgumentException();
			for(List<Pair<Integer,Integer>> stroke : character) {
				ArrayList<Node> cur = new ArrayList<Node>(stroke.size());
				for(Pair<Integer,Integer> p : stroke) cur.add(new Node(((double)p.x)/width,((double)p.y)/height));
				nodes.add(cur);
			}
		}
		{
			Node prev = null;
			for(int sid = 0; sid < nodes.size(); sid++) {
				List<Pair<Integer,Pair<Node,Node>>> nodePairs = new LinkedList<>();
				List<Node> stroke = nodes.get(sid);
				getVertex(stroke,0,stroke.size()-1,0,nodePairs);
				makeVertexFeature(sid, nodePairs);
				if (prev != null) {
					makeMoveFeature(sid, prev, stroke.get(0));
				}
				prev = stroke.get(stroke.size()-1);
			}
		}
		addFeature(2000000, nodes.size());
		addFeature(2000000 + nodes.size(), 10);
		
		Collections.sort(featureNodes);
		featureNodes.add(new FeatureNode(-1, 0.0));
	}
	public static double distance(Node p1, Node p2) {
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	// return index of best node and minimal distance
	private static Pair<Integer,Double> minimumDistance(List<Node> nodes) {
		if(nodes.size()<2) return new Pair<>(-1,0.0);
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
