package be.mapariensis.kanjiryoku.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IntegerSpiral implements Iterator<Integer> {
	private final int min, max;
	private int curStep = 0;
	private int curSign = -1;
	private int prev;
	private int next;
	private boolean hasMore = true, cacheStale = true;

	public IntegerSpiral(int center, int min, int max) {
		this.min = min;
		this.max = max;
		this.prev = center;
		if (center > max || center < min) {
			throw new IllegalArgumentException(String.format(
					"Invalid center, min, max: %d,%d,%d", center, min, max));
		}
	}

	@Override
	public boolean hasNext() {
		if (!hasMore)
			return false;
		if (cacheStale) {
			return hasMore = computeNext();
		} else {
			return true;
		}
	}

	@Override
	public Integer next() {
		// the hasNext() call also computes and caches the next element
		if (!hasNext())
			throw new NoSuchElementException();
		curStep++;
		curSign = -curSign;
		prev = next;
		cacheStale = true;
		return next;
	}

	// Compute the next element, and return false if iterator is exhausted
	private boolean computeNext() {
		cacheStale = false;
		int next = prev + curSign * curStep;
		if (next >= min && next <= max) {
			this.next = next;
			return true;
		} else {
			// skip one step
			curSign = -curSign;
			curStep++;
			int nextnext = next + curSign * curStep;
			if (nextnext >= min && nextnext <= max) {
				this.next = nextnext;
				return true;
			}
			// if this one fails, we're permanently out of bounds
			return false;
		}
	}

	@Override
	public void remove() { }

}
