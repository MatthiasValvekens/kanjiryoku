package be.mapariensis.kanjiryoku.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class IntegerSpiralTest {

	private static <T> List<T> asList(Iterator<T> iter) {
		ArrayList<T> result = new ArrayList<T>();
		while (iter.hasNext()) {
			result.add(iter.next());
		}
		return result;
	}

	@Test
	public void testEdge() {
		List<Integer> expected = Arrays.asList(9);
		IntegerSpiral spiral = new IntegerSpiral(9, 9, 9);
		assertEquals(expected, asList(spiral));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadParameters1() {
		new IntegerSpiral(9, 10, 11);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadParameters2() {
		new IntegerSpiral(12, 11, 9);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadParameters3() {
		new IntegerSpiral(12, 10, 11);
	}

	@Test
	public void stateTest1() {
		IntegerSpiral spiral = new IntegerSpiral(9, 5, 11);
		assertEquals(true, spiral.hasNext());
		assertEquals(true, spiral.hasNext());
	}

	@Test
	public void stateTest2() {
		IntegerSpiral spiral = new IntegerSpiral(9, 5, 11);
		asList(spiral); // exhaust iterator
		assertEquals(false, spiral.hasNext());
		assertEquals(false, spiral.hasNext());
	}

	@Test
	public void generalCase1() {
		List<Integer> expected = Arrays.asList(9, 10, 8, 11, 7, 6, 5);
		IntegerSpiral spiral = new IntegerSpiral(9, 5, 11);
		assertEquals(expected, asList(spiral));
	}

	@Test
	public void generalCase2() {
		List<Integer> expected = Arrays.asList(9, 10, 8, 11, 12, 13, 14, 15);
		IntegerSpiral spiral = new IntegerSpiral(9, 8, 15);
		assertEquals(expected, asList(spiral));
	}

}
