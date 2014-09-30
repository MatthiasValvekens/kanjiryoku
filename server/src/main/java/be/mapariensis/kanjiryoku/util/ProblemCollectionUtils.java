package be.mapariensis.kanjiryoku.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.collections4.list.SetUniqueList;

import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.model.ProblemOrganizer;

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
}
