package be.mapariensis.kanjiryoku.providers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import be.mapariensis.kanjiryoku.Constants;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.model.Word;
import be.mapariensis.kanjiryoku.model.YojiProblem;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public class KanjiryokuYojiWrapper implements ProblemParser {
	public static final String PARAM_DICTFILE = "dictfile";
	public static final String PARAM_ENCODING = "encoding";
	public static final String PARAM_SEED = "seed";
	public static final String PARAM_OPTIONTOTAL = "optionCount";
	public static final int PARAM_OPTIONTOTAL_DEFAULT = 4;
	private final KanjiryokuShindanParser kparser = new KanjiryokuShindanParser();
	private final String kanjilist; // TODO : this is a temporary workaround 
	private final Random rng;
	private final int optiontotal;
	public KanjiryokuYojiWrapper(String kanjilist, Random rng, int optiontotal) {
		this.kanjilist = kanjilist;
		this.rng = rng;
		this.optiontotal = optiontotal;
	}
	@Override
	public Problem parseProblem(String input) throws ParseException {
		Problem p = kparser.parseProblem(input);
		// sanity checks, extract the first (and only) word
		Word w;
		if(p.words.size() != 1 || (w = p.words.get(0)).main.length()!=4) throw new ParseException("Not a Yoji problem", 0);
		
		// TODO : pick options that actually make sense for the "wrong" suggestions
		List<List<String>> options = new ArrayList<List<String>>(4);
		for(int i = 0; i<4; i++) {
			List<String> myoptions = new ArrayList<String>(optiontotal);
			int correctPosition = rng.nextInt(optiontotal);
			for(int j = 0; j<optiontotal; j++) {
				if(j == correctPosition) {
					myoptions.add(new String(new char[] {w.main.charAt(i)}));
				} else {
					myoptions.add(new String(new char[] {kanjilist.charAt(rng.nextInt(kanjilist.length()))}));
				}
			}
		}
		return new YojiProblem(w, options);
	}
	public static class Factory implements ProblemParserFactory {
		@Override
		public KanjiryokuYojiWrapper getParser(IProperties params)
				throws BadConfigurationException {
			if(params == null) throw new BadConfigurationException("KanjiryokuYojiWrapper requires parameters, but none were supplied.");
			String dictfile = params.getRequired(PARAM_DICTFILE, String.class);
			Charset enc = Charset.forName(params.getRequired(PARAM_ENCODING, String.class));
			Random rng = new Random(params.getSafely(PARAM_SEED,Integer.class,(int)(System.currentTimeMillis()%10000)));
			int optiontotal = params.getSafely(PARAM_OPTIONTOTAL,Integer.class,PARAM_OPTIONTOTAL_DEFAULT);
			StringBuilder dict = new StringBuilder();
			
			// build kanji list
			List<String> strings;
			try {
				strings = Files.readAllLines(Paths.get(dictfile), enc);
			} catch (IOException ex) {
				throw new BadConfigurationException(ex);
			}
			for(String s : strings) {
				if(!s.startsWith(Constants.COMMENT_PREFIX)) {
					dict.append(s);
				}
			}
			return new KanjiryokuYojiWrapper(dict.toString(),rng,optiontotal);
		}
		
	}
}