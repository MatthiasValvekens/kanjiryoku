package be.mapariensis.kanjiryoku.net.input;

import javax.swing.JPanel;

public abstract class InputComponent extends JPanel {
	public abstract InputHandler getInputHandler();
	public abstract void endProblem();
	public abstract void setLock(boolean locked);
}
