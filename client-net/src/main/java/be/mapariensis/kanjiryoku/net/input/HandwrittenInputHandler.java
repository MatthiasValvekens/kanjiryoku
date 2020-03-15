package be.mapariensis.kanjiryoku.net.input;

import java.util.List;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.net.exceptions.ServerSubmissionException;

public interface HandwrittenInputHandler extends InputHandler {
	void sendStroke(List<Dot> dots) throws ServerSubmissionException;

}
