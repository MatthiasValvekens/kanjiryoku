package be.mapariensis.kanjiryoku.problemsets;

import static be.mapariensis.kanjiryoku.config.ConfigFields.*;
import static be.mapariensis.kanjiryoku.util.ProblemCollectionUtils.*;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.server.ConnectionMonitor;
import be.mapariensis.kanjiryoku.problemsets.organizers.CategoryOrganizer;
import be.mapariensis.kanjiryoku.providers.ProblemParserFactory;
import be.mapariensis.kanjiryoku.util.IProperties;

public class ProblemSetManagerImpl implements ProblemSetManager {
	private static final Logger log = LoggerFactory.getLogger(ProblemSetManagerImpl.class);
	private final int problemsPerCategory;
	private final boolean resetDifficulty;
	private final String fileNameFormat;
	private final String digitFormat;
	private final int minDiff;
	private final int maxDiff;
	private final String enc;
	

	
	public ProblemSetManagerImpl(String fileNameFormat, String digitFormat, int problemsPerCategory,
			boolean resetDifficulty, int minDiff, int maxDiff, String enc) {
		this.problemsPerCategory = problemsPerCategory;
		this.resetDifficulty = resetDifficulty;
		this.minDiff = minDiff;
		this.maxDiff = maxDiff;
		this.fileNameFormat = fileNameFormat;
		this.digitFormat = digitFormat;
		this.enc = enc;
	}
	
	private final Map<String,RatedProblemList> psets = new HashMap<String,RatedProblemList>();
	private final Object LOCK = new Object();
	
	
	
	
	@Override
	public void loadNewConfig(String name, IProperties config) throws BadConfigurationException {
		synchronized(LOCK) {
			
			// allow for overrides for most configuration
			String digitFormat = config.getTyped(FILE_NAME_DIFFICULTY_FORMAT, String.class,this.digitFormat);
			int minDiff = config.getTyped(MIN_DIFFICULTY, Integer.class, this.minDiff);
			int maxDiff = config.getTyped(MAX_DIFFICULTY, Integer.class, this.maxDiff);
			String enc = config.getTyped(FILE_ENCODING, String.class, this.enc);
			String fileNameFormat = config.getTyped(FILE_NAME_FORMAT,String.class,this.fileNameFormat);
			
			// load problem parser factory
			String parserFactory = config.getRequired(PARSER_FACTORY_CLASS, String.class);
			ProblemParserFactory ppf;
			try {
				ppf = (ProblemParserFactory) ConnectionMonitor.class.getClassLoader().loadClass(parserFactory).newInstance();
			} catch(Exception e) {
				log.error("Failed to instantiate problem parser factory {}",parserFactory);
				throw new BadConfigurationException(e);
			}
			
			// This parameter isn't required, but some parser factories may fail when not supplied with parameters
			IProperties ppfConfig = config.getTyped(PARSER_FACTORY_SETTINGS, IProperties.class);
			RatedProblemList problems;
			try {
				problems = readRatedProblems(ppf.getParser(ppfConfig),fileNameFormat, name, digitFormat, minDiff, maxDiff, Charset.forName(enc));
			} catch (ParseException e) {
				throw new BadConfigurationException(e);
			}
			psets.put(name,problems);
		}
	}
	@Override
	public ProblemOrganizer getProblemSets(int seed, List<String> names) throws BadConfigurationException {
		// seed and build organizer (allows for resource sharing)
		List<RatedProblemList> sets = new ArrayList<RatedProblemList>(names.size());
		for(String name : names) {
			RatedProblemList rpl = psets.get(name);
			if(rpl == null) throw new BadConfigurationException(new StringBuilder("Problem set ").append(name).append(" does not exist.").toString());
			sets.add(rpl);
		}
		return new CategoryOrganizer(sets, problemsPerCategory, new Random(seed), resetDifficulty, minDiff, maxDiff);
	}
	
}
