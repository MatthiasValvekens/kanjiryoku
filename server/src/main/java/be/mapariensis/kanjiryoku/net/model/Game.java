package be.mapariensis.kanjiryoku.net.model;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static be.mapariensis.kanjiryoku.config.ConfigFields.*;
import be.mapariensis.kanjiryoku.cr.KanjiGuesser;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UnsupportedGameException;
import be.mapariensis.kanjiryoku.net.server.GameServerInterface;
import be.mapariensis.kanjiryoku.net.server.games.TakingTurnsServer;
import be.mapariensis.kanjiryoku.problemsets.ProblemSetManager;
import be.mapariensis.kanjiryoku.util.IProperties;

public enum Game {
    TAKINGTURNS {
        @Override
        public GameServerInterface getServer(IProperties config,
                KanjiGuesser guesser, ProblemSetManager psets)
                throws BadConfigurationException {
            int seed = (int) (System.currentTimeMillis() % 10000);
            log.info("Starting game with seed {}", seed);
            @SuppressWarnings("unchecked")
            List<String> names = config.getRequired(CATEGORY_LIST, List.class);
            boolean batonPass = config.getSafely(ENABLE_BATON_PASS,
                    Boolean.class, ENABLE_BATON_PASS_DEFAULT);
            return new TakingTurnsServer(psets.getProblemSets(seed, names),
                    guesser, batonPass);
        }

        @Override
        public String toString() {
            return "Turn-based Guessing";
        }
    };
    private static final Logger log = LoggerFactory.getLogger(Game.class);

    public abstract GameServerInterface getServer(IProperties config,
            KanjiGuesser guesser, ProblemSetManager psets)
            throws UnsupportedGameException, ServerBackendException,
            BadConfigurationException;
}
