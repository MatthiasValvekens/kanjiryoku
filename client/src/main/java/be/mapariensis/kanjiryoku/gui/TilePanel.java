package be.mapariensis.kanjiryoku.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import be.mapariensis.kanjiryoku.gui.utils.TextRendering;

public class TilePanel extends JPanel implements TiledInputInterface {
	private static final double TILE_SCALE = 0.9;
	private static final Color selectionColor = TextRendering.rgb(41, 82, 229,(float)0.4);
	private class Tile extends JPanel {
		final String content;
		public Tile(String s) {
			this.content = s;
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent ev) {
					// TODO : interact with server here
				}
				
				@Override
				public void mouseEntered(MouseEvent ev) {
					if(!locked) {
						highlighted = true;
						repaint();
					}
				}
				
				@Override
				public void mouseExited(MouseEvent ev) {
					if(!locked) {
						highlighted = false;
						repaint();
					}
				}
			});
		}
		
		private Font f = null;
		private int fsize = 0;
		private int swidth = 0;
		private volatile Dimension size;
		private volatile boolean highlighted;
		@Override
		public void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			Rectangle rect = g2d.getClipBounds();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setColor(highlighted ? selectionColor : Color.WHITE);
			g2d.fillRect(rect.x,rect.y,rect.width,rect.height);
			g2d.setColor(Color.BLACK);
			Rectangle contentClip = scale(rect,TILE_SCALE);
			// compute font size
			// start by trying biggest font size possible
			
			// TODO optimize for identical of tiles with the same content length?
			if(f == null || !getSize().equals(size)) {
				fsize = contentClip.height/2;
				FontMetrics fm;
				do {
					fm = g2d.getFontMetrics(f = new Font(TextRendering.FONT_NAME,Font.PLAIN,fsize--));
				} while ((swidth = fm.stringWidth(content)) > contentClip.width);
			}
			g2d.setFont(f);
			g2d.drawString(content, contentClip.x+contentClip.width/2 - swidth/2 , contentClip.y+contentClip.height/2+fsize/3);
			size = getSize();
		}
		
	}
	private volatile boolean locked;
	public static Rectangle scale(Rectangle r, double factor) {
		int newwidth = (int) (r.width * factor);
		int newheight = (int) (r.height * factor);
		return new Rectangle(r.x+(r.width - newwidth)/2, r.y+(r.height - newheight)/2,newwidth,newheight);
	}
	public TilePanel() {
		setBackground(Color.WHITE);
	}
	private volatile int rowcount;
	@Override
	public void tileSelected(int i) {
		
	}
	private List<Tile> tiles;
	@Override
	public void setTiles(List<String> tiles) {
		rowcount = (int) Math.ceil(Math.sqrt(tiles.size()));
		GridLayout layout = new GridLayout(rowcount, rowcount, 0, 0);
		setLayout(layout);
		this.tiles = new ArrayList<Tile>(tiles.size());
		for(String s : tiles) {
			Tile t = new Tile(s);
			add(t);
			this.tiles.add(t);
		}
		revalidate();
		repaint();
	}
	
	@Override
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
}
