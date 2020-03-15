package be.mapariensis.kanjiryoku.gui;

import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.input.InputHandler;

public interface GameClientInterface {
	String getUsername();

	void setLock(boolean locked);

	void deliverAnswer(boolean correct, char displayChar);

	void setProblem(Problem p);

	Problem getProblem();

	InputHandler getInputHandler();
}
