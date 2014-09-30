package be.mapariensis.kanjiryoku.net.server.organizers;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.model.ProblemOrganizer;
import be.mapariensis.kanjiryoku.net.model.RatedProblem;
import be.mapariensis.kanjiryoku.net.model.RatedProblemList;
import static be.mapariensis.kanjiryoku.net.model.RatedProblem.*;

public class CategoryOrganizer implements ProblemOrganizer {
	public final List<RatedProblemList> cats;
	public final int perCategory;
	private int curCatIx = 0;
	private int problemsSet = 0;
	private int difficulty = 1;
	private final Random rng;
	public CategoryOrganizer(List<RatedProblemList> cats,
			int perCategory, Random rng) {
		this.cats = cats;
		this.perCategory = perCategory;
		this.rng = rng;
	}
	@Override
	public boolean hasNext() {
		if(problemsSet < Math.min(perCategory,cats.get(curCatIx).size())) return true;
		// end of category reached
		return curCatIx < cats.size()-1 && Math.min(perCategory,cats.get(curCatIx+1).size())>0;  
	}
	@Override
	public Problem next(boolean lastAnswer) {
		difficulty = restrict(difficulty+ (lastAnswer ? 1 : -1), MIN_DIFFICULTY, MAX_DIFFICULTY);
		if(!hasNext()) throw new IllegalStateException();
		if(problemsSet < Math.min(perCategory,cats.get(curCatIx).size())) {
			// next problem in category
			problemsSet++;
		} else {
			// hasNext returned true, so this will work
			curCatIx++; // move to next category
		}
		// find a problem of the appropriate difficulty
		RatedProblemList scope = cats.get(curCatIx);
		Iterator<Integer> iter = new IntegerSpiral(difficulty,MIN_DIFFICULTY,MAX_DIFFICULTY);
		List<RatedProblem> restrictedScope;
		// iterate through possible difficulty levels
		do {
			int difficulty = iter.next();
			restrictedScope = scope.extractDifficulty(difficulty);
		} while(iter.hasNext() && restrictedScope.isEmpty());
		if(restrictedScope.isEmpty()) throw new IllegalStateException();
		return restrictedScope.remove(rng.nextInt(restrictedScope.size())).problem; // return a problem and remove it from the set of candidates
	}
	
	private static int restrict(int x, int min, int max) {
		return Math.max(min, Math.min(x, max));
	}
	
	private static class IntegerSpiral implements Iterator<Integer> {
		final int center, min,max;
		int curStep = 0;
		int curSign = -1;
		
		private IntegerSpiral(int center, int min, int max) {
			this.center = center;
			this.min = min;
			this.max = max;
		}
		@Override
		public boolean hasNext() {
			int next = computeNext();
			return next>=min && next<=max;
		}

		@Override
		public Integer next() {
			int retval = computeNext();
			curStep++;
			curSign = -curSign;
			return retval;
		}
		private int computeNext() {
			return center+curSign*curStep;
		}
		@Override
		public void remove() {
			return;
		}
		
	}

	
}