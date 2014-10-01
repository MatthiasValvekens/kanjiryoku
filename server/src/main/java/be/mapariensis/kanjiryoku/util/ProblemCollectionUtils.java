package be.mapariensis.kanjiryoku.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.model.ProblemWithBlank;
import be.mapariensis.kanjiryoku.problemsets.ProblemOrganizer;
import be.mapariensis.kanjiryoku.problemsets.RatedProblem;
import be.mapariensis.kanjiryoku.problemsets.RatedProblemList;
import be.mapariensis.kanjiryoku.problemsets.organizers.CategoryOrganizer;
import be.mapariensis.kanjiryoku.providers.KanjiryokuShindanParser;
import be.mapariensis.kanjiryoku.providers.ProblemParser;

public class ProblemCollectionUtils {
	private static final Logger log = LoggerFactory.getLogger(ProblemCollectionUtils.class);
	// convenience method, use only on sets of relatively small files
	public static Collection<String> collectFromFiles(Collection<String> filenames, int fromEachFile, int totalSample, Charset encoding, Random rng) throws IOException {
		int maxAvailable = fromEachFile*filenames.size();
		if( maxAvailable < totalSample) throw new IllegalArgumentException("Not enough data to sample");
		Collection<String> result=SetUniqueList.setUniqueList(new ArrayList<String>());
		ArrayList<String> pool = new ArrayList<String>(maxAvailable);
		for(String s : filenames) {
			List<String> strings = Files.readAllLines(Paths.get(s), encoding);
			Set<String> fromThisFile = new HashSet<String>();
			while(fromThisFile.size()<fromEachFile) fromThisFile.add(strings.get(rng.nextInt(strings.size())));
			pool.addAll(fromThisFile);
		}
		while(result.size()<totalSample)
			result.add(pool.get(rng.nextInt(pool.size())));
		return result;
	}
	
	public static ProblemOrganizer fromIterator(final Iterator<Problem> iter) {
		return new ProblemOrganizer() {
			
			@Override
			public Problem next(boolean lastAnswer) {
				return iter.next();
			}
			
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}
		};
	}
	public static ProblemOrganizer buildKanjiryokuShindanOrganizer(String format, List<String> categoryNames, String digitFormat,int problemsPerCategory, int minDiff, int maxDiff, boolean resetDifficulty,Random rng) throws IOException, ParseException {
		ProblemParser<ProblemWithBlank> parser = new KanjiryokuShindanParser();
		List<RatedProblemList> cats = new ArrayList<RatedProblemList>(categoryNames.size());
		for(String categoryName : categoryNames) {
			List<RatedProblem> currentCategory = new ArrayList<RatedProblem>();
			for(int i = minDiff;i<=maxDiff;i++){
				String fname=buildFilename(format, categoryName, i, digitFormat);
				log.info("Loading problems from {}",fname);
				List<String> strings = Files.readAllLines(Paths.get(fname), Charset.forName("Shift-JIS"));
				for(String s : strings) {
					if(!s.startsWith("//"))
						currentCategory.add(new RatedProblem(parser.parseProblem(s), i));
				}
			}
			cats.add(new RatedProblemList(currentCategory));
		}


		return new CategoryOrganizer(cats, problemsPerCategory, rng,resetDifficulty,minDiff,maxDiff);
	}
	
	public static String buildFilename(String format, String category, int difficulty, String digitFormat) {
		HashMap<String,String> values = new HashMap<String,String>();
		values.put(ConfigFields.FILE_NAME_CATEGORY_VAR,category);
		values.put(ConfigFields.FILE_NAME_DIFFICULTY_VAR, String.format(digitFormat,difficulty));
		StrSubstitutor subber = new StrSubstitutor(values);
		return subber.replace(format);
	}
}
