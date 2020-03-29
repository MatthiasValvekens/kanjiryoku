package be.mapariensis.kanjiryoku.gui;

import java.awt.Graphics2D;
import java.util.List;

import be.mapariensis.kanjiryoku.model.Problem;

public interface ProblemRenderer {
    char FILLER_CHAR = '○';
    void drawProblem(Graphics2D g2d, List<Character> correctInputs,
            Problem problem, Character lastWrongInput);
}
