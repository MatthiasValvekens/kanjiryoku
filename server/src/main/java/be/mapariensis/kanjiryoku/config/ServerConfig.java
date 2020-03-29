package be.mapariensis.kanjiryoku.config;

import javax.sql.DataSource;

import be.mapariensis.kanjiryoku.cr.KanjiGuesserFactory;
import be.mapariensis.kanjiryoku.problemsets.ProblemSetManager;
import be.mapariensis.kanjiryoku.util.IProperties;

public interface ServerConfig extends IProperties {
    ProblemSetManager getProblemSetManager();

    KanjiGuesserFactory getKanjiGuesserFactory();

    DataSource getDbConnection();
}