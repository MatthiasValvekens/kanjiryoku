package be.mapariensis.kanjiryoku.gui;

import java.util.List;

public interface MultipleChoiceInputInterface {
	public void optionSelected(int i);
	public void setOptions(List<String> options);
	public void clearSelection();
	public String optionContent(int i);
}
