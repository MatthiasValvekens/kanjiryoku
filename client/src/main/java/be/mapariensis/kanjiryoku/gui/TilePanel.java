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

import javax.swing.JComponent;
import be.mapariensis.kanjiryoku.gui.utils.TextRendering;
import be.mapariensis.kanjiryoku.net.input.InputComponent;
import be.mapariensis.kanjiryoku.net.input.MultipleChoiceInputHandler;
import be.mapariensis.kanjiryoku.net.input.MultipleChoiceInputHandlerImpl;

public class TilePanel extends InputComponent implements MultipleChoiceInputInterface {
	private static final double TILE_SCALE = 0.9;
	private static final Color selectionColor = TextRendering.rgb(41, 82, 229,(float)0.4);
	private static final Color highlightColor = TextRendering.rgb(41, 82, 229,(float)0.2);
	private class Tile extends JComponent {
		final String content;
		final int id;
		public Tile(String s, int i) {
			this.content = s;
			this.id = i;
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent ev) {
					// TODO : interact with server here
					optionSelected(id);
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
		private volatile boolean selected;
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			Rectangle rect = g2d.getClipBounds();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			if(highlighted || selected) {
				g2d.setColor(selected ? selectionColor : highlightColor);
				g2d.fillRect(rect.x,rect.y,rect.width,rect.height);
			}
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
	private final MultipleChoiceInputHandler ih;
	public static Rectangle scale(Rectangle r, double factor) {
		int newwidth = (int) (r.width * factor);
		int newheight = (int) (r.height * factor);
		return new Rectangle(r.x+(r.width - newwidth)/2, r.y+(r.height - newheight)/2,newwidth,newheight);
	}
	public TilePanel(GUIBridge bridge) {
		this.ih = new MultipleChoiceInputHandlerImpl(bridge, this);
		setBackground(Color.WHITE);
	}
	private volatile int rowcount;
	private volatile int selectedTile = -1;
	@Override
	public void optionSelected(int i) {
		if(i<0 || i>=tiles.size()) throw new IllegalArgumentException();
		clearSelection();
		Tile t = tiles.get(selectedTile = i);
		t.selected = true;
		t.repaint();
	}
	@Override
	public void clearSelection() {
		if(selectedTile != -1) {
			Tile t = tiles.get(selectedTile);
			t.selected = false;
			t.repaint();
		}
		selectedTile = -1;
	}
	private List<Tile> tiles;
	@Override
	public void setOptions(List<String> tiles) {
		rowcount = (int) Math.ceil(Math.sqrt(tiles.size()));
		GridLayout layout = new GridLayout(rowcount, rowcount, 0, 0);
		setLayout(layout);
		this.tiles = new ArrayList<Tile>(tiles.size());
		for(int i = 0;i<tiles.size();i++) {
			Tile t = new Tile(tiles.get(i),i);
			add(t);
			this.tiles.add(t);
		}
		revalidate();
		repaint();
	}
	

	@Override
	public MultipleChoiceInputHandler getInputHandler() {
		return ih;
	}
	@Override
	public void endProblem() {
		
	}
	@Override
	public void setLock(boolean locked) {
		this.locked = locked;
	}
	@Override
	public String optionContent(int i) {
		return tiles.get(i).content;
	}


}
