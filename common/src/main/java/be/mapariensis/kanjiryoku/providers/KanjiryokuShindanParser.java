package be.mapariensis.kanjiryoku.providers;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.model.KakiProblem;
import be.mapariensis.kanjiryoku.model.ProblemWithBlank;
import be.mapariensis.kanjiryoku.model.Word;
import be.mapariensis.kanjiryoku.model.YomiProblem;
import be.mapariensis.kanjiryoku.util.IProperties;

public class KanjiryokuShindanParser implements ProblemParser {
    private static final Logger log = LoggerFactory
            .getLogger(KanjiryokuShindanParser.class);
    private static final char BLANK_DELIMITER = '（';

    @Override
    public ProblemWithBlank parseProblem(String input) throws ParseException {
        int parserPos = 0;
        int blankPos = -1;
        int wordIx = 0;
        List<Word> words = new ArrayList<Word>();
        boolean main = false; // furigana comes first
        boolean yomi = false;
        String furigana = null;
        while (parserPos < input.length()) {
            // read item
            char cur = input.charAt(parserPos);
            int itemEnd, nextItem;
            char match;
            // add kana combinations as words
            if (isKana(cur)) {
                int oldPos = parserPos;
                // aggregate kana word
                while (parserPos < input.length()
                        && isKana(cur = input.charAt(parserPos))) {
                    parserPos++;
                }
                words.add(new Word(input.substring(oldPos, parserPos)));
                wordIx++;
                continue; // skip rest
            }
            if (cur == BLANK_DELIMITER) {
                if (blankPos != -1)
                    throw new ParseException("Multiple blanks", parserPos);
                blankPos = wordIx;
                yomi = !main; // if the main part is blank, it's a writing
                                // problem
            }
            // find matching delimiter if applicable
            if ((match = matchDelim(cur)) != 0) {
                itemEnd = findNext(input, match, parserPos);
                nextItem = itemEnd + 1; // skip ahead to first character after
                                        // matching delim
                parserPos++; // move parser position to item after opening
                                // delimiter
            } else {
                // no delimiter found
                itemEnd = scanUntilDelimOrEnd(input, parserPos);
                nextItem = itemEnd;
            }

            if (main) {
                assert furigana != null;
                words.add(new Word(input.substring(parserPos, itemEnd),
                        furigana));
                wordIx++;
            } else {
                furigana = input.substring(parserPos, itemEnd);
            }
            main = !main;
            parserPos = nextItem;
        }
        if (blankPos == -1) {
            log.error("No blank in problem \"{}\"", input);
            throw new ParseException("No blank in problem", -1);
        }
        return yomi ? new YomiProblem(words, blankPos) : new KakiProblem(words,
                blankPos);
    }

    private static int findNext(String string, char c, int start)
            throws ParseException {
        for (int i = start; i < string.length(); i++) {
            if (string.charAt(i) == c)
                return i;
        }
        throw new ParseException("Unmatched delimiter " + c, start);
    }

    private static int scanUntilDelimOrEnd(String string, int start) {
        for (int i = start; i < string.length(); i++) {
            char c = string.charAt(i);
            if (isKana(c) || matchDelim(c) != 0)
                return i;
        }
        return string.length();
    }

    private static char matchDelim(char c) {
        switch (c) {
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
        return b == Character.UnicodeBlock.HIRAGANA
                || b == Character.UnicodeBlock.KATAKANA;
    }

    public static class Factory implements ProblemParserFactory {

        @Override
        public KanjiryokuShindanParser getParser(IProperties params) {
            return new KanjiryokuShindanParser();
        }

    }
}
