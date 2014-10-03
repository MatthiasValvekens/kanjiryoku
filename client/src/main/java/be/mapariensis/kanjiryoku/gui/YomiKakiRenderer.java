package be.mapariensis.kanjiryoku.gui;

import java.awt.Graphics2D;
import java.util.List;

import be.mapariensis.kanjiryoku.gui.utils.TextRendering;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.model.ProblemWithBlank;
import be.mapariensis.kanjiryoku.model.Word;

public abstract class YomiKakiRenderer implements ProblemRenderer {
	@Override
	public final void drawProblem(Graphics2D g, List<Character> correctInputs, Problem p, Character lastWrongInput) {
		ProblemWithBlank problem = (ProblemWithBlank)p;
		for(Word w : problem) {
			int newpos;
			if(problem.isBlank(w)) {
				// figure out how much filler we need
				StringBuilder sb = new StringBuilder();
				int solLength = problem.getFullSolution().length();
				for(char c : correctInputs) {
					sb.append(c);
					solLength--;
				}
				assert solLength >= 0;
				if(solLength > 0 && lastWrongInput != null) {
					sb.append(lastWrongInput);
					solLength--;
				}
				for(;solLength>0;solLength--) {
					sb.append(FILLER_CHAR);
				}
				String sequenced = sb.toString();
				newpos = TextRendering.renderWord(g,getBlankMain(sequenced,w), getBlankRuby(sequenced,w), lastWrongInput == null ? null : getMistakeMarker());
			} else {
				newpos = TextRendering.renderWord(g, w);
			}
			g.translate(newpos,0);
		}
	}
	
	protected abstract String getBlankRuby(String sequenced,Word w);
	protected abstract String getBlankMain(String sequenced,Word w);
	protected abstract TextRendering.MistakeMarker getMistakeMarker();
}
