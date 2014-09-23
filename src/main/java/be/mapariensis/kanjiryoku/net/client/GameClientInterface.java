package be.mapariensis.kanjiryoku.net.client;

import java.text.ParseException;
import java.util.List;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.gui.ProblemRenderer;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;


public interface GameClientInterface {
	public String getUsername();
	public DrawingPanelInterface getCanvas();
	public void sendStroke(List<Dot> dots);
	public void clearInput();
	public void deliverAnswer(boolean correct, char displayChar);
	public Problem parseProblem(String s) throws ParseException;
	public void setProblem(Problem p);
	public <T extends Problem> ProblemRenderer<T> getRenderer(T problem);
	public void consumeActiveResponseHandler(NetworkMessage msg) throws ClientException;
}
