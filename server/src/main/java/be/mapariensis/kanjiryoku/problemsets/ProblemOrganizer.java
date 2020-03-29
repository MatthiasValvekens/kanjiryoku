package be.mapariensis.kanjiryoku.problemsets;

import be.mapariensis.kanjiryoku.model.Problem;

public interface ProblemOrganizer {
    boolean hasNext();

    Problem next(boolean lastAnswer);

    String getCategoryName();
}
