package be.mapariensis.kanjiryoku.gui;

import java.util.List;

import be.mapariensis.kanjiryoku.cr.Dot;

public interface DrawingPanelInterface {
	void drawStroke(List<Dot> dots);

	void clearStrokes();

	int getWidth();

	int getHeight();
}
