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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.gui.utils.FadingOverlay;


public class DrawPanel extends JPanel implements DrawingPanelInterface {
	private static final Logger log = LoggerFactory.getLogger(DrawPanel.class);
	private static final BufferedImage checkmarkImage;
	private static final String CHECKMARK_FILE = "checkmark.png";
	static {
		BufferedImage thing = null;
		try(InputStream is = DrawPanel.class.getClassLoader().getResourceAsStream(CHECKMARK_FILE)) {
				thing = ImageIO.read(is);
		} catch (IOException | IllegalArgumentException e) {
			log.warn("Failed to read checkmark.png",e);
		}
		checkmarkImage = thing;
	}
	private final List<List<Dot>> strokes = new LinkedList<List<Dot>>();
	private List<Dot> currentStroke;
	private final GameClientInterface server;
	private static final int BRUSH_RADIUS = 10;
	private static final Stroke BRUSH_STROKE = new BasicStroke(BRUSH_RADIUS, BasicStroke.CAP_ROUND,BasicStroke.JOIN_MITER);
	private final Dimension size;
	private boolean locked = true;
	private final FadingOverlay fader = new FadingOverlay(this, checkmarkImage);
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
		fader.startFade();
	}

	@Override
	public Dimension getSize() {
		return size;
	}
	@Override
	public Dimension getPreferredSize() {
		return size;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
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
		fader.paint(g2d);
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
				log.debug("\nFinished stroke {}: {} ",strokes.size(), currentStroke);
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
	public void setLock(boolean locked) {
		this.locked = locked;
	}
}
