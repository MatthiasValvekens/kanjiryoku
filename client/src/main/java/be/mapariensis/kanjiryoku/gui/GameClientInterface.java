package be.mapariensis.kanjiryoku.gui;

import java.text.ParseException;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.input.InputHandler;


public interface GameClientInterface {
	public String getUsername();
	public void setLock(boolean locked);
	public void deliverAnswer(boolean correct, char displayChar);
	public Problem parseProblem(String s) throws ParseException;
	public void setProblem(Problem p);
	public Problem getProblem();
	public InputHandler getInputHandler();
	public void inputCleared();
}
