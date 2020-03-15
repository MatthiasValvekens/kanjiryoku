package be.mapariensis.kanjiryoku.util;

import be.mapariensis.kanjiryoku.Constants;
import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.problemsets.RatedProblem;
import be.mapariensis.kanjiryoku.problemsets.RatedProblemList;
import be.mapariensis.kanjiryoku.providers.ProblemParser;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProblemCollectionUtils {
	private static final Logger log = LoggerFactory
			.getLogger(ProblemCollectionUtils.class);

	public static RatedProblemList readRatedProblems(ProblemParser parser,
			String format, String categoryName, String digitFormat,
			int minDiff, int maxDiff, Charset enc) throws ParseException {
		List<RatedProblem> currentCategory = new ArrayList<>();
		for (int i = minDiff; i <= maxDiff; i++) {
			String fname = buildFilename(format, categoryName, i, digitFormat);
			log.info("Loading problems from {}", fname);
			List<String> strings;
			try {
				strings = Files.readAllLines(Paths.get(fname), enc);
			} catch (IOException ex) {
				log.warn("Failed to read file {}", fname, ex);
				continue;
			}
			for (String s : strings) {
				if (!s.startsWith(Constants.COMMENT_PREFIX))
					currentCategory.add(new RatedProblem(
							parser.parseProblem(s), i));
			}
		}
		return new RatedProblemList(currentCategory);
	}

	public static String buildFilename(String format, String category,
			int difficulty, String digitFormat) {
		HashMap<String, String> values = new HashMap<>();
		values.put(ConfigFields.FILE_NAME_CATEGORY_VAR, category);
		values.put(ConfigFields.FILE_NAME_DIFFICULTY_VAR,
				String.format(digitFormat, difficulty));
		StrSubstitutor subber = new StrSubstitutor(values);
		return subber.replace(format);
	}
}
