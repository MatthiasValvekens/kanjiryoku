package be.mapariensis.kanjiryoku.persistent.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StatementIndexerTest {

	@Test
	public void testIndex1() {
		String input = "${testvar} blah blah ${testvar2}";
		StatementIndexer si = new StatementIndexer(input);
		assertEquals(si.getSql(), "? blah blah ?");
		int i = si.getIndex().get("testvar");
		assertEquals(i, 1);
		i = si.getIndex().get("testvar2");
		assertEquals(i, 2);
		assertEquals(si.getIndex().get("blah"), null);
	}

	@Test
	public void testIndex2() {
		String input = "select id from users where username=${username}";
		StatementIndexer si = new StatementIndexer(input);
		assertEquals(si.getSql(), "select id from users where username=?");
		int i = si.getIndex().get("username");
		assertEquals(i, 1);
		assertEquals(si.getIndex().get("id"), null);
	}

}
