package be.mapariensis.kanjiryoku.problemsets.organizers;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.problemsets.ProblemOrganizer;
import be.mapariensis.kanjiryoku.problemsets.RatedProblem;
import be.mapariensis.kanjiryoku.problemsets.RatedProblemList;
import be.mapariensis.kanjiryoku.util.IntegerSpiral;

public class SingleCategoryOrganizer implements ProblemOrganizer {
	public final RatedProblemList rpl;

	private final String name;
	private final int maxProblems;
	private int problemsSet = 0;
	private int difficulty;
	private final Random rng;
	private final int minDifficulty, maxDifficulty;

	private boolean cacheStale = true;

	private boolean hasMore = true;
	private Problem nextUp, nextDown;

	public SingleCategoryOrganizer(String name, RatedProblemList rpl,
			int maxProblems, Random rng, int minDifficulty, int maxDifficulty) {
		this.name = name;
		this.rpl = rpl;
		this.maxProblems = maxProblems;
		this.rng = rng;
		this.minDifficulty = minDifficulty;
		this.maxDifficulty = maxDifficulty;
		this.difficulty = minDifficulty;
	}

	public void setDifficulty(int diff) {
		if (diff < minDifficulty || diff > maxDifficulty)
			throw new IllegalArgumentException();
		if (difficulty != diff)
			cacheStale = true;
		difficulty = diff;
	}

	@Override
	public boolean hasNext() {
		if (!hasMore)
			return false;
		if (cacheStale) {
			nextUp = computeNext(true);
			nextDown = computeNext(false);
			return hasMore = (nextUp != null || nextDown != null);
		} else {
			return true;
		}
	}

	@Override
	public Problem next(boolean lastAnswer) {
		// check & cache
		if (!hasNext())
			throw new NoSuchElementException();
		cacheStale = true;
		problemsSet++;
		if (lastAnswer) {
			if (nextUp != null)
				return nextUp;
			else
				return nextDown;
		} else {
			if (nextDown != null)
				return nextDown;
			else
				return nextUp;
		}
	}

	private Problem computeNext(boolean lastAnswer) {
		cacheStale = false;
		if (problemsSet >= Math.min(maxProblems, rpl.size()))
			return null;
		difficulty = restrict(difficulty + (lastAnswer ? 1 : -1),
				minDifficulty, maxDifficulty);
		// find a problem of the appropriate difficulty
		Iterator<Integer> iter = new IntegerSpiral(difficulty, minDifficulty,
				maxDifficulty);
		List<RatedProblem> restrictedScope;
		// iterate through possible difficulty levels
		do {
			difficulty = iter.next();
			restrictedScope = rpl.extractDifficulty(difficulty);
		} while (iter.hasNext() && restrictedScope.isEmpty());
		// restrictedScope now contains all problems of suitable difficulty
		// next time, the difficulty counter will start at this level
		if (restrictedScope.isEmpty())
			return null; // no suitable problems
		// return a problem and remove it from the set of candidates
		return restrictedScope.remove(rng.nextInt(restrictedScope.size())).problem;
	}

	private static int restrict(int x, int min, int max) {
		return Math.max(min, Math.min(x, max));
	}

	@Override
	public String getCategoryName() {
		return name;
	}
}
