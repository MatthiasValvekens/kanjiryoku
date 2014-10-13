package be.mapariensis.kanjiryoku.model;

import java.util.Arrays;
import java.util.List;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public class YojiProblem extends KakiProblem implements MultipleChoiceOptions {
	public static final String PROPERTY_WORD = "word";
	public static final String PROPERTY_OPTIONS = "options";
	private final List<List<String>> options;

	public YojiProblem(Word yoji, List<List<String>> options) {
		super(Arrays.asList(yoji), 0);
		this.options = options;
		if(yoji.main.length() != 4 || options.size()>4) throw new IllegalArgumentException("A yoji problem must have length 4.");
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
	public static YojiProblem fromProperties(IProperties props) throws BadConfigurationException {
		List<String> word = props.getRequired(PROPERTY_WORD, List.class);
		return new YojiProblem(new Word(word.get(0),word.get(1)),props.getRequired(PROPERTY_OPTIONS, List.class));
	}
}
