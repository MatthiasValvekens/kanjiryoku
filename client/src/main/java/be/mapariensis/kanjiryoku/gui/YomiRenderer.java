package be.mapariensis.kanjiryoku.gui;

import be.mapariensis.kanjiryoku.gui.utils.TextRendering.MistakeMarker;
import be.mapariensis.kanjiryoku.model.Word;
import be.mapariensis.kanjiryoku.model.YomiProblem;

public class YomiRenderer extends YomiKakiRenderer {

	@Override
	protected MistakeMarker getMistakeMarker() {
		return MistakeMarker.FURIGANA;
	}

	@Override
	protected String getBlankRuby(String sequenced, Word w) {
		return sequenced;
	}

	@Override
	protected String getBlankMain(String sequenced, Word w) {
		return w.main;
	}
}
