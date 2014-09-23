package be.mapariensis.kanjiryoku.net.client;

import java.util.List;

import be.mapariensis.kanjiryoku.cr.Dot;

public interface DrawingPanelInterface {
	public void drawStroke(List<Dot> dots);
	public void clearStrokes();
	public void setLock(boolean locked);
}
