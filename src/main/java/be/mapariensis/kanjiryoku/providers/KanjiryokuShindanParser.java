package be.mapariensis.kanjiryoku.providers;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import be.mapariensis.kanjiryoku.model.Word;
import be.mapariensis.kanjiryoku.model.YomiKakiProblem;

public class KanjiryokuShindanParser implements ProblemProvider<YomiKakiProblem> {
	private static final char BLANK_DELIMITER = '（';
	private final Collection<YomiKakiProblem> problems;
	public KanjiryokuShindanParser(Collection<String> input) throws ParseException {
		ArrayList<YomiKakiProblem> problems = new ArrayList<YomiKakiProblem>(input.size());
		for(String s : input) {
			problems.add(parseProblem(s));
		}
		this.problems = Collections.unmodifiableCollection(problems);
	}
	@Override
	public Collection<YomiKakiProblem> getProblems() {
		return problems;
	}
	public static YomiKakiProblem parseProblem(String input) throws ParseException {
		int parserPos = 0;
		int blankPos = -1;
		int wordIx = 0;
		List<Word> words = new ArrayList<Word>();
		boolean main = false; // furigana comes first
		boolean yomi = false;
		String furigana = null;
		while(parserPos < input.length()) {
			//read item
			char cur = input.charAt(parserPos);
			int itemEnd,nextItem;
			char match;
			//add kana combinations as words
			if(isKana(cur)) {
				int oldPos = parserPos;
				//aggregate kana word
				while(parserPos<input.length() && isKana(cur=input.charAt(parserPos))) {
					parserPos++;
				}
				words.add(new Word(input.substring(oldPos,parserPos)));
				wordIx++;
				continue; //skip rest
			}
			if(cur == BLANK_DELIMITER) {
				if(blankPos != -1) throw new ParseException("Multiple blanks",parserPos);
				blankPos = wordIx;
				yomi = !main; // if the main part is blank, it's a writing problem
			}
			//find matching delimiter if applicable
			if((match = matchDelim(cur)) != 0) {
				itemEnd = findNext(input, match, parserPos);
				nextItem = itemEnd+1; // skip ahead to first character after matching delim
				parserPos++; //move parser position to item after opening delimiter 
			} else {
				// no delimiter found
				itemEnd = scanUntilDelimOrEnd(input, parserPos);
				nextItem = itemEnd;
			}
			
			if(main){
				assert furigana != null;
				words.add(new Word(input.substring(parserPos, itemEnd),furigana));
				wordIx++;
			} else {
				furigana = input.substring(parserPos, itemEnd);
			}
			main = !main;
			parserPos = nextItem;
		}
		if(blankPos == -1) throw new ParseException("No blank in problem",-1);
		return new YomiKakiProblem(words, blankPos, yomi);
	}
	private static int findNext(String string, char c, int start) throws ParseException {
		for(int i = start; i<string.length(); i++) {
			if(string.charAt(i)==c) return i;
		}
		throw new ParseException("Unmatched delimiter "+c,start);
	}
	private static int scanUntilDelimOrEnd(String string, int start) {
		for(int i = start; i<string.length();i++) {
			char c = string.charAt(i);
			if(isKana(c) || matchDelim(c) != 0)  return i;
		}
		return string.length();
	}
	private static char matchDelim(char c) {
		switch(c) {
		case '（':
			return '）';
		case '｛':
			return '｝';
		case '［':
			return '］';
		default:
			return 0;
		}
		
	}
	private static boolean isKana(char c) {
		Character.UnicodeBlock b = Character.UnicodeBlock.of(c);
		return b==Character.UnicodeBlock.HIRAGANA || b==Character.UnicodeBlock.KATAKANA;
	}
}
