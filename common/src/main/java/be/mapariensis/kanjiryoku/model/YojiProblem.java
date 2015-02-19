package be.mapariensis.kanjiryoku.model;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.providers.ProblemParser;
import be.mapariensis.kanjiryoku.util.IProperties;
import be.mapariensis.kanjiryoku.util.IPropertiesImpl;

public class YojiProblem extends KakiProblem implements MultipleChoiceOptions {
	public static final String PROPERTY_WORD = "word";
	public static final String PROPERTY_OPTIONS = "options";
	private final List<List<String>> options;

	public YojiProblem(Word yoji, List<List<String>> options) {
		super(Arrays.asList(yoji), 0);
		this.options = options;
		if (yoji.main.length() != 4)
			throw new IllegalArgumentException(
					"A yoji problem must have length 4.");
		if (options.size() == 0)
			throw new IllegalArgumentException(
					"A yoji problem must have at least one option available.");
	}

	@Override
	public InputMethod getInputMethod() {
		return InputMethod.MULTIPLE_CHOICE;
	}

	@Override
	public List<String> getOptions(int position) {
		return options.get(position);
	}

	@SuppressWarnings("unchecked")
	public static YojiProblem fromProperties(IProperties props)
			throws BadConfigurationException {
		List<String> word = props.getRequired(PROPERTY_WORD, List.class);
		return new YojiProblem(new Word(word.get(0), word.get(1)),
				props.getRequired(PROPERTY_OPTIONS, List.class));
	}

	@Override
	public String toString() {
		JSONObject json = new JSONObject();
		Word w = words.get(0);
		json.put(PROPERTY_WORD, Arrays.asList(w.main, w.furigana));
		json.put(PROPERTY_OPTIONS, options);
		return json.toString();
	}

	public static class JSONYojiParser implements ProblemParser {

		@Override
		public Problem parseProblem(String input) throws ParseException {
			try {
				return fromProperties(new IPropertiesImpl(new JSONObject(input)));
			} catch (JSONException | BadConfigurationException ex) {
				throw new ParseException("Invalid input: " + input, 0);
			}
		}

	}
}
