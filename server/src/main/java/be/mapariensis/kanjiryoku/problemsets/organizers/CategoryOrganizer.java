package be.mapariensis.kanjiryoku.problemsets.organizers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.problemsets.ProblemOrganizer;
import be.mapariensis.kanjiryoku.problemsets.RatedProblemList;

public class CategoryOrganizer implements ProblemOrganizer {
	Logger log = LoggerFactory.getLogger(CategoryOrganizer.class);
	private final List<SingleCategoryOrganizer> cats;

	private int curCatIx = 0;
	private int difficulty;
	private final boolean resetDifficulty;
	private final int minDifficulty, maxDifficulty;

	private boolean hasMore = true;

	public CategoryOrganizer(Map<String, RatedProblemList> cats,
			int perCategory, Random rng, boolean resetDifficulty,
			int minDifficulty, int maxDifficulty) {
		this.cats = new ArrayList<SingleCategoryOrganizer>(cats.size());
		for (Map.Entry<String, RatedProblemList> entry : cats.entrySet()) {
			this.cats
					.add(new SingleCategoryOrganizer(entry.getKey(), entry
							.getValue(), perCategory, rng, minDifficulty,
							maxDifficulty));
		}
		this.resetDifficulty = resetDifficulty;
		this.minDifficulty = minDifficulty;
		this.maxDifficulty = maxDifficulty;
		this.difficulty = minDifficulty;
	}

	@Override
	public boolean hasNext() {
		if (hasMore == false)
			return false;

		if (cats.get(curCatIx).hasNext())
			return true;

		// end of category reached
		log.debug("No more problems in current category.");
		if (resetDifficulty) {
			difficulty = minDifficulty;
		}
		while (curCatIx < cats.size() - 1) {
			curCatIx++;
			// difficulty SHOULD not matter for hasNext
			// but it's better to play it safe
			SingleCategoryOrganizer sco = cats.get(curCatIx);
			sco.setDifficulty(difficulty);
			if (cats.get(curCatIx).hasNext())
				return true;
		}
		// no non-empty categories found. Bail
		return (hasMore = false);
	}

	@Override
	public Problem next(boolean lastAnswer) {
		// check & cache
		if (!hasNext())
			throw new NoSuchElementException();
		// category may report different difficulty level, depending on
		// circumstances
		difficulty = restrict(difficulty + (lastAnswer ? 1 : -1),
				minDifficulty, maxDifficulty);
		return cats.get(curCatIx).next(lastAnswer);

	}

	private static int restrict(int x, int min, int max) {
		return Math.max(min, Math.min(x, max));
	}

	@Override
	public String getCategoryName() {
		SingleCategoryOrganizer sco = cats.get(curCatIx);
		return sco.getCategoryName();
	}
}
