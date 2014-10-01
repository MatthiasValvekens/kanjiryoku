package be.mapariensis.kanjiryoku.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.collections4.list.SetUniqueList;

import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.model.ProblemWithBlank;
import be.mapariensis.kanjiryoku.problemsets.ProblemOrganizer;
import be.mapariensis.kanjiryoku.problemsets.RatedProblem;
import be.mapariensis.kanjiryoku.problemsets.RatedProblemList;
import be.mapariensis.kanjiryoku.problemsets.organizers.CategoryOrganizer;
import be.mapariensis.kanjiryoku.providers.KanjiryokuShindanParser;
import be.mapariensis.kanjiryoku.providers.ProblemParser;

public class ProblemCollectionUtils {
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
	
	private static final String format = "data\\problems\\my_%s_%02d.txt";
	private static final int difficultyLevels=11;
	public static ProblemOrganizer buildKanjiryokuShindanOrganizer(int problemsPerCategory, Random rng) throws IOException, ParseException {
		ProblemParser<ProblemWithBlank> parser = new KanjiryokuShindanParser();
		List<RatedProblem> kaki = new ArrayList<RatedProblem>();
		List<RatedProblem> yomi = new ArrayList<RatedProblem>();
		for(int i = 1;i<=difficultyLevels;i++){
			// read kaki
			String fname=String.format(format,"kaki",i);
			List<String> strings = Files.readAllLines(Paths.get(fname), Charset.forName("Shift-JIS"));
			for(String s : strings) {
				if(!s.startsWith("//"))
					kaki.add(new RatedProblem(parser.parseProblem(s), i));
			}
			// read yomi
			fname=String.format(format,"yomi",i);
			strings = Files.readAllLines(Paths.get(fname), Charset.forName("Shift-JIS"));
			for(String s : strings) {
				if(!s.startsWith("//"))
					yomi.add(new RatedProblem(parser.parseProblem(s), i));
			}
		}
		List<RatedProblemList> cats = new ArrayList<RatedProblemList>(2);
		cats.add(new RatedProblemList(yomi));
		cats.add(new RatedProblemList(kaki));
		return new CategoryOrganizer(cats, problemsPerCategory, rng);
	}
}
