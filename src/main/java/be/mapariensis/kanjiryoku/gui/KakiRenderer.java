package be.mapariensis.kanjiryoku.gui;

import java.awt.Graphics2D;

import be.mapariensis.kanjiryoku.gui.utils.TextRendering;
import be.mapariensis.kanjiryoku.model.KakiProblem;
import be.mapariensis.kanjiryoku.model.Word;

public class KakiRenderer extends ProblemRenderer<KakiProblem> {
	@Override
	public void drawProblem(Graphics2D g) {
		KakiProblem problem = getProblem();
		for(Word w : problem) {
			int newpos;
			if(problem.isBlank(w)) {
				// figure out how much filler we need
				StringBuilder sb = new StringBuilder();
				int solLength = getSolution().length();
				for(char c : getCorrectInputs()) {
					sb.append(c);
					solLength--;
				}
				assert solLength >= 0;
				for(;solLength>0;solLength--) {
					sb.append(FILLER_CHAR);
				}
				newpos = TextRendering.renderWord(g,sb.toString(), w.furigana);
			} else {
				newpos = TextRendering.renderWord(g, w);
			}
			g.translate(newpos,0);
		}
	}
}
