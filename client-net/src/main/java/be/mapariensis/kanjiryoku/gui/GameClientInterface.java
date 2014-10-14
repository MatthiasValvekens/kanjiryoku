package be.mapariensis.kanjiryoku.gui;

import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.input.InputHandler;


public interface GameClientInterface {
	public String getUsername();
	public void setLock(boolean locked);
	public void deliverAnswer(boolean correct, char displayChar);
	public void setProblem(Problem p);
	public Problem getProblem();
	public InputHandler getInputHandler();
}
