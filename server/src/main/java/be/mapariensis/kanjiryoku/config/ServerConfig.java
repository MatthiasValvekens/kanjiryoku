package be.mapariensis.kanjiryoku.config;

import be.mapariensis.kanjiryoku.cr.KanjiGuesserFactory;
import be.mapariensis.kanjiryoku.problemsets.ProblemSetManager;
import be.mapariensis.kanjiryoku.util.IProperties;

public interface ServerConfig extends IProperties {
	public ProblemSetManager getProblemSetManager();

	public KanjiGuesserFactory getKanjiGuesserFactory();

}