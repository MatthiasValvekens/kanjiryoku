package be.mapariensis.kanjiryoku.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Path2D;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.net.client.DrawingPanelInterface;
import be.mapariensis.kanjiryoku.net.client.GameClientInterface;


public class DrawPanel extends JPanel implements DrawingPanelInterface {
	private static final Logger log = LoggerFactory.getLogger(DrawPanel.class);
	private final List<List<Dot>> strokes = new LinkedList<List<Dot>>();
	private List<Dot> currentStroke;
	private final GameClientInterface server;
	private static final int BRUSH_RADIUS = 10;
	private static final Stroke BRUSH_STROKE = new BasicStroke(BRUSH_RADIUS, BasicStroke.CAP_ROUND,BasicStroke.JOIN_MITER);
	private final Dimension size;
	private boolean locked = true, solvedProblem = false;
	public DrawPanel(Dimension size, GameClientInterface server) {
		this.server = server;
		this.size = size;
		DrawingListener listener = new DrawingListener();
		addMouseListener(listener);
		addMouseMotionListener(listener);
		clearStrokes();
	}
	
	public void endProblem() {
		setLock(true);
		solvedProblem = true;
		repaint();
	}
	
	@Override
	public Dimension getSize() {
		return size;
	}
	@Override
	public Dimension getPreferredSize() {
		return size;
	}
//	public List<Character> getChars() {
//		//return server.guess(size.width, size.height,strokes, Constants.TOLERANCE);
//		
//	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if(solvedProblem) {
			// TODO: do something nice here
		}
		Rectangle frame = g.getClipBounds();
		if(!(g instanceof Graphics2D)) throw new IllegalArgumentException("Require Graphics2D");
		Graphics2D g2d = (Graphics2D) g.create(frame.x, frame.y, frame.width, frame.height); //normalize graphics to current clip
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0,0,frame.width,frame.height);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
		//paint strokes
		g2d.setColor(Color.BLACK);
		g2d.setStroke(BRUSH_STROKE);
		for(List<Dot> stroke : strokes) {
			// connect stroke parts with lines
			Path2D path = new Path2D.Float();
			boolean first = true;
			for(Dot dot : stroke) {
				if(first) { // performance-wise this is the safest
					//set path anchor
					path.moveTo(dot.x, dot.y);
					first = false;
				} else {
					path.lineTo(dot.x, dot.y);
				}
			}
			g2d.draw(path);
		}
		g2d.dispose();
	}
	
	@Override
	public void clearStrokes() {
		strokes.clear();
		currentStroke = new LinkedList<Dot>();
		// add first stroke to list
		strokes.add(currentStroke);
		repaint();
	}
	
	
	private class DrawingListener implements MouseMotionListener, MouseListener {

		@Override
		public void mouseDragged(MouseEvent e) {
			if(!locked && SwingUtilities.isLeftMouseButton(e)) {
				currentStroke.add(new Dot(e.getX(),e.getY()));
				repaint();
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if(!locked && SwingUtilities.isRightMouseButton(e)) {
				clearStrokes();
				server.clearInput();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {
			if(!locked && SwingUtilities.isLeftMouseButton(e)) {
				currentStroke.add(new Dot(e.getX(),e.getY()));
				log.info("\nFinished stroke {}: {} ",strokes.size(), currentStroke);
				server.sendStroke(currentStroke);
				
				currentStroke = new LinkedList<Dot>();
				strokes.add(currentStroke);
				repaint();
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mouseMoved(MouseEvent e) {
		}		
	}


	@Override
	public synchronized void drawStroke(List<Dot> dots) {
		// don't check lock status here
		strokes.add(dots);
		repaint();
	}
	@Override
	public void setLock(boolean locked) {
		log.info("Panel lock set to {}", locked ? "locked" : "released");
		this.locked = locked;
	}
}
