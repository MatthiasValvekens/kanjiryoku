package be.mapariensis.kanjiryoku.cr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.chasen.crfpp.Recognizer;
import org.chasen.crfpp.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

/**
 * A frontend to the Zinnia handwriting recognition engine.
 * 
 * @author Matthias Valvekens
 * @version 1.0
 */
public class ZinniaGuesser implements KanjiGuesser {
	private static final Logger log = LoggerFactory
			.getLogger(ZinniaGuesser.class);
	private final Recognizer eng;
	private final int tolerance;

	public static class Factory implements KanjiGuesserFactory {
		static {
			loadLibraries();
		}
		private Recognizer eng;

		@Override
		public KanjiGuesser getGuesser(IProperties config)
				throws BadConfigurationException, IOException {
			int tolerance = config.getTyped(ConfigFields.CR_TOLERANCE,
					Integer.class, ConfigFields.CR_TOLERANCE_DEFAULT);
			if (eng == null) {
				String modelfile = config.getRequired(ConfigFields.MODEL_FILE,
						String.class);
				Recognizer r = new Recognizer();
				r.open(modelfile);
				eng = r; // avoid invalid state if open throws an exception
			}
			return new ZinniaGuesser(eng, tolerance);
		}

	}

	private static void loadLibraries() {
		try {
			System.loadLibrary("jzinnia-0.06-JAVA");
		} catch (UnsatisfiedLinkError err) {
			log.error("Failed to load Zinnia library.", err);
		}
	}

	private ZinniaGuesser(Recognizer eng, int tolerance) {
		if (eng == null)
			throw new IllegalArgumentException();
		this.eng = eng;
		this.tolerance = tolerance;
	}

	public ZinniaGuesser(String modelfile, int tolerance) throws IOException {
		this.eng = new Recognizer();
		this.tolerance = tolerance;
		if (!eng.open(modelfile))
			throw new IOException("Failed to read model file.");
	}

	@Override
	public List<Character> guess(int width, int height, List<List<Dot>> strokes) {
		if (strokes.isEmpty())
			throw new IllegalArgumentException();
		// Zinnia character model
		org.chasen.crfpp.Character charmodel = new org.chasen.crfpp.Character();
		charmodel.set_width(width);
		charmodel.set_height(height);
		// add strokes
		final int strokeCount = strokes.size();
		for (int i = 0; i < strokeCount; i++) {
			List<Dot> stroke = strokes.get(i);
			for (Dot d : stroke) {
				charmodel.add(i, d.x, d.y);
			}
		}
		Result res;
		// synchronize on the recognizer, just to be safe TODO: determine
		// whether this is necessary
		synchronized (eng) {
			res = eng.classify(charmodel, tolerance);
		}

		// process Zinnia result
		List<Character> chars = new ArrayList<Character>(tolerance);
		for (int i = 0; i < tolerance; i++) {
			String value = res.value(i);
			float score = res.score(i);
			log.debug("Candidate {} is {} with score {}", i + 1, value, score);
			chars.add(value.charAt(0));
		}
		return chars;
	}

	@Override
	public String toString() {
		return "Zinnia engine";
	}

	@Override
	public void close() {
	}
}
