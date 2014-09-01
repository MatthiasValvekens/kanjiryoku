package be.mapariensis.kanjiryoku.providers;

import static org.junit.Assert.*;

import java.text.ParseException;

import org.junit.Test;

import be.mapariensis.kanjiryoku.model.Word;
import be.mapariensis.kanjiryoku.model.YomiKakiProblem;

public class KanjiryokuShindanParserTest {

	@Test
	public void testParse1() throws ParseException {
		String input = "［え］干［と］支は｛うし｝（丑）";
		YomiKakiProblem problem = KanjiryokuShindanParser.parseProblem(input);
		assertEquals(new Word("干","え"),problem.words.get(0));
		assertFalse(problem.yomi);
		assertEquals(3,problem.blankIndex);
	}
	@Test
	public void testParse2() throws ParseException {
		String input = "（ひと）｛一｝つだけ［のこ］残る";
		YomiKakiProblem problem = KanjiryokuShindanParser.parseProblem(input);
		assertEquals(problem.words.get(1),new Word("つだけ"));
		assertEquals(new Word("一","ひと"),problem.words.get(0));
		assertEquals(0,problem.blankIndex);
		assertTrue(problem.yomi);
	}

}
