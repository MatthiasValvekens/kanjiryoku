package be.mapariensis.kanjiryoku.net.commands;

import be.mapariensis.kanjiryoku.model.YojiProblem.JSONYojiParser;
import be.mapariensis.kanjiryoku.providers.KanjiryokuShindanParser;
import be.mapariensis.kanjiryoku.providers.ProblemParser;

public enum ParserName {
	
	KS_PARSER {		
		@Override
		public KanjiryokuShindanParser getParser() {
			return ksp;
		}
	}, YOJI_PARSER {
		@Override
		public ProblemParser getParser() {
			return yojiParser;
		}
	};
	private static KanjiryokuShindanParser ksp = new KanjiryokuShindanParser();
	private static ProblemParser yojiParser = new JSONYojiParser();
	public abstract ProblemParser getParser();
}
