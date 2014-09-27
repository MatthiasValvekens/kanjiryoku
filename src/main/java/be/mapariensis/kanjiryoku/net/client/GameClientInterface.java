package be.mapariensis.kanjiryoku.net.client;

import java.text.ParseException;
import java.util.List;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.model.Problem;


public interface GameClientInterface {
	public String getUsername();
	public void setLock(boolean locked);
	public DrawingPanelInterface getCanvas();
	public void sendStroke(List<Dot> dots);
	public void clearInput();
	public void deliverAnswer(boolean correct, char displayChar);
	public Problem parseProblem(String s) throws ParseException;
	public void setProblem(Problem p);
}
