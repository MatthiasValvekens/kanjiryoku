package be.mapariensis.kanjiryoku.net.input;

public interface MultipleChoiceInputHandler extends InputHandler {
	void broadcastSelect(int choice);
}
