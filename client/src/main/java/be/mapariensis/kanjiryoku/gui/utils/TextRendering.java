package be.mapariensis.kanjiryoku.gui.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.model.Word;

public class TextRendering {
	public static final String FONT_NAME = "Meiryo";
	public static enum MistakeMarker {
		FURIGANA, MAIN
	}
	private static final Logger log = LoggerFactory.getLogger(TextRendering.class);
	public static int renderWord(Graphics2D g2d, Word w) {
		return renderWord(g2d, w.main, w.furigana,null);
	}
	private final static int normalSize = 24;
	private final static int rubySize=normalSize/2;
	public static int renderWord(Graphics2D g2d, String main, String furigana, MistakeMarker mark) {
		Graphics2D g = (Graphics2D) g2d.create();
		g2d = null;
		Rectangle rect = g.getClipBounds();
		
		Font normal = new Font(FONT_NAME,Font.PLAIN,normalSize);
		Font ruby = new Font(FONT_NAME,Font.PLAIN,rubySize);
		
		//calculate main width
		FontMetrics metrics = g.getFontMetrics(normal);
		int mainWidth = metrics.stringWidth(main);
		
		//calculate furigana width
		
		FontMetrics rubyMetrics = g.getFontMetrics(ruby);
		
		int rubyWidth = rubyMetrics.stringWidth(furigana);
		int width = Math.max(rubyWidth, mainWidth)+5; //include a few pixels for padding
		log.debug("Widths: Ruby: {}, Normal: {}",rubyWidth, mainWidth);
		if(mainWidth > rect.width)
			log.warn("Clip too small to contain text ({} pixels short).",mainWidth-rect.width);
		//make sure text is centered
		int mainPos = (width - mainWidth)/2;
		int rubyPos = (width - rubyWidth)/2;
		g.setFont(ruby);
		if(mark == MistakeMarker.FURIGANA) {
			g.setColor(Color.RED);
			g.drawString(furigana, rubyPos, normalSize/3);
			g.setColor(Color.BLACK);
		} else {
			g.drawString(furigana, rubyPos, normalSize/3);
		}
		g.setFont(normal);
		if(mark == MistakeMarker.MAIN) {
			g.setColor(Color.RED);
			g.drawString(main,mainPos,rect.height/3);
			g.setColor(Color.BLACK);
		} else {
			g.drawString(main,mainPos,rect.height/3);
		}
		
		g.dispose();
		
		return width;
	}
	
	public static Color rgb(int r, int g, int b, float alpha){
		return new Color((float)(r/256.0),(float)(g/256.0),(float)(b/256.0),alpha);
	}
}
