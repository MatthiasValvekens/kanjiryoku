package be.mapariensis.kanjiryoku.problemsets;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class RatedProblemList implements List<RatedProblem> {
	private final List<RatedProblem> backend;

	/**
	 * Input list should be sorted.
	 */
	public RatedProblemList(List<RatedProblem> backend) {
		this.backend = backend;
	}

	@Override
	public void add(int index, RatedProblem element) {
		backend.add(index, element);
	}

	@Override
	public boolean add(RatedProblem e) {
		return backend.add(e);
	}

	@Override
	public boolean addAll(Collection<? extends RatedProblem> c) {
		return backend.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends RatedProblem> c) {
		return backend.addAll(index, c);
	}

	@Override
	public void clear() {
		backend.clear();
	}

	@Override
	public boolean contains(Object o) {
		return backend.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return backend.containsAll(c);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RatedProblemList that = (RatedProblemList) o;
		return backend.equals(that.backend);
	}

	@Override
	public RatedProblem get(int index) {
		return backend.get(index);
	}

	@Override
	public int hashCode() {
		return backend.hashCode();
	}

	@Override
	public int indexOf(Object o) {
		return backend.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return backend.isEmpty();
	}

	@Override
	public Iterator<RatedProblem> iterator() {
		return backend.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return backend.lastIndexOf(o);
	}

	@Override
	public ListIterator<RatedProblem> listIterator() {
		return backend.listIterator();
	}

	@Override
	public ListIterator<RatedProblem> listIterator(int index) {
		return backend.listIterator(index);
	}

	@Override
	public RatedProblem remove(int index) {
		return backend.remove(index);
	}

	@Override
	public boolean remove(Object o) {
		return backend.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return backend.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return backend.retainAll(c);
	}

	@Override
	public RatedProblem set(int index, RatedProblem element) {
		return backend.set(index, element);
	}

	@Override
	public int size() {
		return backend.size();
	}

	@Override
	public List<RatedProblem> subList(int fromIndex, int toIndex) {
		return backend.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return backend.toArray();
	}

	@SuppressWarnings("SuspiciousToArrayCall")
	@Override
	public <T> T[] toArray(T[] a) {
		return backend.toArray(a);
	}

	public List<RatedProblem> extractDifficulty(int difficulty) {
		int lower = indexedBinarySearch(difficulty);
		if (lower < 0)
			return Collections.emptyList();
		int upper = indexedBinarySearch(difficulty + 1);
		if (upper < 0)
			upper = backend.size();
		if (upper <= lower)
			throw new IllegalStateException();
		return backend.subList(lower, upper);
	}

	private int indexedBinarySearch(int key) {
		int low = 0;
		int high = this.size() - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			RatedProblem midVal = this.get(mid);
			int cmp = Integer.compare(midVal.difficulty, key);
			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1); // key not found
	}
}
