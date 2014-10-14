package be.mapariensis.kanjiryoku.net.commands;

import be.mapariensis.kanjiryoku.providers.KanjiryokuShindanParser;
import be.mapariensis.kanjiryoku.providers.ProblemParser;

public enum ParserName {
	
	KS_PARSER {		
		@Override
		public KanjiryokuShindanParser getParser() {
			return ksp;
		}
	};
	private static KanjiryokuShindanParser ksp = new KanjiryokuShindanParser();
	public abstract ProblemParser getParser();
}
