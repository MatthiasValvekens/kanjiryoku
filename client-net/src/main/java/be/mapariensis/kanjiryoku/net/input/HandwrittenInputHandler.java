package be.mapariensis.kanjiryoku.net.input;

import java.util.List;

import be.mapariensis.kanjiryoku.cr.Dot;

public interface HandwrittenInputHandler extends InputHandler {
	void sendStroke(List<Dot> dots);

}
