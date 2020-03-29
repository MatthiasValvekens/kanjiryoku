package be.mapariensis.kanjiryoku.gui;

import java.util.List;

public interface MultipleChoiceInputInterface {
    void optionSelected(int i);

    void setOptions(List<String> options);

    void clearSelection();
}
