package be.mapariensis.kanjiryoku.gui;

import java.awt.Graphics2D;
import java.util.List;

import be.mapariensis.kanjiryoku.gui.utils.TextRendering;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.model.Word;
import be.mapariensis.kanjiryoku.model.YomiProblem;

public class YomiRenderer implements ProblemRenderer {
	@Override
	public void drawProblem(Graphics2D g, List<Character> correctInputs, Problem p) {
		YomiProblem problem =(YomiProblem) p;
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
				for(;solLength>0;solLength--) {
					sb.append(FILLER_CHAR);
				}
				newpos = TextRendering.renderWord(g,w.main, sb.toString());
			} else {
				newpos = TextRendering.renderWord(g, w);
			}
			g.translate(newpos,0);
		}
	}

	@Override
	public Class<YomiProblem> getProblemClass() {
		return YomiProblem.class;
	}
}
