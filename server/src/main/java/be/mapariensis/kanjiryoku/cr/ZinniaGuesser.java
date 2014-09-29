package be.mapariensis.kanjiryoku.cr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.chasen.crfpp.Recognizer;
import org.chasen.crfpp.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A frontend to the Zinnia handwriting recognition engine.
 * @author Matthias Valvekens
 * @version 1.0
 */
public class ZinniaGuesser implements KanjiGuesser {
	private static final Logger log = LoggerFactory.getLogger(ZinniaGuesser.class);
	private final Recognizer eng;
	public ZinniaGuesser(Recognizer eng) {
		this.eng = eng;
	}
	public ZinniaGuesser(String modelfile) throws IOException {
		this.eng = new Recognizer();
		if(!eng.open(modelfile)) throw new IOException("Failed to read model file.") ;
	}
	@Override
	public List<Character> guess(int width,int height, List<List<Dot>> strokes, int numCandidates) {
		// Zinnia character model
		org.chasen.crfpp.Character charmodel = new org.chasen.crfpp.Character();
		charmodel.set_width(width);
		charmodel.set_height(height);
		//add strokes
		final int strokeCount = strokes.size();
		for(int i = 0; i<strokeCount; i++) {
			List<Dot> stroke = strokes.get(i);
			for(Dot d : stroke) {
				charmodel.add(i, d.x, d.y);
			}
		}
		Result res;
		// synchronize on the recognizer, just to be safe
		synchronized(eng) {
			res = eng.classify(charmodel, numCandidates);
		}
		
		//process Zinnia result
		List<Character> chars = new ArrayList<Character>(numCandidates);
		for(int i = 0; i<numCandidates; i++) {
			String value = res.value(i);
			float score = res.score(i);
			log.info("Candidate {} is {} with score {}",i+1,value,score);
			chars.add(value.charAt(0));
		}
		return chars;
	}
	@Override
	public String toString() {
		return "Zinnia engine";
	}
}
