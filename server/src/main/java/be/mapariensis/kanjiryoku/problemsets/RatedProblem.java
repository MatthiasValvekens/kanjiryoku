package be.mapariensis.kanjiryoku.problemsets;

import be.mapariensis.kanjiryoku.model.Problem;

public class RatedProblem implements Comparable<RatedProblem>{
	public static final int MAX_DIFFICULTY = 11;
	public static final int MIN_DIFFICULTY = 1;
	public final Problem problem;
	public final int difficulty;
	public RatedProblem(Problem problem, int difficulty) {
		this.problem = problem;
		this.difficulty = restrict(difficulty,MIN_DIFFICULTY,MAX_DIFFICULTY);
	}
	private static int restrict(int x, int min, int max) {
		return Math.max(min, Math.min(x, max));
	}
	@Override
	public int compareTo(RatedProblem o) {
		return Integer.compare(difficulty, o.difficulty);
	}
}
