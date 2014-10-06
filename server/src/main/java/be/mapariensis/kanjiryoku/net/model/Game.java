package be.mapariensis.kanjiryoku.net.model;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static be.mapariensis.kanjiryoku.config.ConfigFields.*;
import be.mapariensis.kanjiryoku.config.IProperties;
import be.mapariensis.kanjiryoku.cr.KanjiGuesser;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.UnsupportedGameException;
import be.mapariensis.kanjiryoku.net.server.GameServerInterface;
import be.mapariensis.kanjiryoku.net.server.games.TakingTurnsServer;
import be.mapariensis.kanjiryoku.problemsets.ProblemOrganizer;
import be.mapariensis.kanjiryoku.util.ProblemCollectionUtils;
public enum Game {
	TAKINGTURNS {
		@Override
		public GameServerInterface getServer(IProperties config, KanjiGuesser guesser)
				throws UnsupportedGameException, ServerBackendException, BadConfigurationException {
			try {
				int seed=(int)(System.currentTimeMillis()%10000);
				log.info("Starting game with seed {}",seed);
				int problemsPerCategory = config.getSafely(PROBLEMS_PER_CATEGORY, Integer.class,PROBLEMS_PER_CATEGORY_DEFAULT);
				String digitFormat = config.getSafely(FILE_NAME_DIFFICULTY_FORMAT, String.class,FILE_NAME_DIFFICULTY_FORMAT_DEFAULT);
				String fileNameFormat = config.getRequired(FILE_NAME_FORMAT, String.class);
				int minDiff = config.getSafely(MIN_DIFFICULTY, Integer.class, MIN_DIFFICULTY_DEFAULT);
				int maxDiff = config.getSafely(MAX_DIFFICULTY, Integer.class, MAX_DIFFICULTY_DEFAULT);
				@SuppressWarnings("unchecked")
				List<String> categoryNames = config.getRequired(CATEGORY_LIST,List.class);
				boolean resetDifficulty = config.getSafely(RESET_AFTER_CATEGORY_SWITCH,Boolean.class,true);
				boolean batonPass = config.getSafely(ENABLE_BATON_PASS, Boolean.class,ENABLE_BATON_PASS_DEFAULT);
				ProblemOrganizer org = ProblemCollectionUtils.buildKanjiryokuShindanOrganizer(fileNameFormat,categoryNames,digitFormat,problemsPerCategory,minDiff,maxDiff, resetDifficulty,new Random(seed));
				return new TakingTurnsServer(org,guesser,batonPass);
			} catch (IOException | ParseException e) {
				throw new ServerBackendException(e);
			}
		};
		
		@Override
		public String toString() {
			return "Turn-based Guessing"; 
		}
	};
	private static final Logger log = LoggerFactory.getLogger(Game.class);
	public abstract GameServerInterface getServer(IProperties config, KanjiGuesser guesser) throws UnsupportedGameException, ServerBackendException, BadConfigurationException;
}
