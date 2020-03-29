package be.mapariensis.kanjiryoku.problemsets;

import be.mapariensis.kanjiryoku.model.Problem;

public class RatedProblem implements Comparable<RatedProblem> {
    public final Problem problem;
    public final int difficulty;

    public RatedProblem(Problem problem, int difficulty) {
        this.problem = problem;
        this.difficulty = difficulty;
    }

    @Override
    public int compareTo(RatedProblem o) {
        return Integer.compare(difficulty, o.difficulty);
    }
}
