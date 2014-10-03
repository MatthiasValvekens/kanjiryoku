package be.mapariensis.kanjiryoku.gui;

import be.mapariensis.kanjiryoku.gui.utils.TextRendering.MistakeMarker;
import be.mapariensis.kanjiryoku.model.KakiProblem;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.model.Word;

public class KakiRenderer extends YomiKakiRenderer {


	@Override
	public Class<? extends Problem> getProblemClass() {
		return KakiProblem.class;
	}


	@Override
	protected MistakeMarker getMistakeMarker() {
		return MistakeMarker.MAIN;
	}


	@Override
	protected String getBlankRuby(String sequenced, Word w) {
		return w.furigana;
	}


	@Override
	protected String getBlankMain(String sequenced, Word w) {
		return sequenced;
	}
}
