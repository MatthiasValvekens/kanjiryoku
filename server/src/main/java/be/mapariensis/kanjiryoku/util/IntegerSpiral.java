package be.mapariensis.kanjiryoku.util;

import java.util.Iterator;

public class IntegerSpiral implements Iterator<Integer> {
	final int min, max;
	int curStep = 0;
	int curSign = -1;
	int prev;

	public IntegerSpiral(int center, int min, int max) {
		this.min = min;
		this.max = max;
		this.prev = center;
	}

	@Override
	public boolean hasNext() {
		int next = computeNext();
		return next >= min && next <= max;
	}

	@Override
	public Integer next() {
		int retval = computeNext();
		curStep++;
		curSign = -curSign;
		prev = retval;
		return retval;
	}

	private int computeNext() {
		return prev + curSign * curStep;
	}

	@Override
	public void remove() {
		return;
	}

}
