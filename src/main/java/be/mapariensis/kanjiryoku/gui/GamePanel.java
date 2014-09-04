package be.mapariensis.kanjiryoku.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.cr.KanjiGuesser;
import be.mapariensis.kanjiryoku.model.*;

public class GamePanel extends JPanel {
	private static final Logger log = LoggerFactory.getLogger(GamePanel.class);
	private static final Map<Class<? extends Problem>, ProblemRenderer<?>> renderers;
	static {
		renderers = new HashMap<Class<? extends Problem>, ProblemRenderer<?>>();
		renderers.put(KakiProblem.class, new KakiRenderer());
		renderers.put(YomiProblem.class, new YomiRenderer());
	}
	@SuppressWarnings("unchecked")
	private static <T extends Problem> ProblemRenderer<T> getRenderer(T problem) {
		for(Class<?> cls : renderers.keySet()) {
			if(cls.isInstance(problem)) {
				ProblemRenderer<T> renderer = (ProblemRenderer<T>) renderers.get(cls);
				renderer.setProblem(problem);
				renderer.setPreferredSize(new Dimension(300,120));
				return renderer;
			}
		}
		throw new IllegalArgumentException("No renderer available.");
	}
	public GamePanel(KanjiGuesser guesser, final Iterator<Problem> problems) {
		setLayout(new BorderLayout());
		class RendererContainer extends JPanel {
			ProblemRenderer<?> renderer;
			Problem problem;
			private void setProblem(Problem problem) {
				this.problem = problem;
				if(this.renderer != null) remove(this.renderer);
				this.renderer = getRenderer(problem);
				add(this.renderer);
			}
		}
		final RendererContainer cont = new RendererContainer();
		if(!problems.hasNext()) throw new IllegalArgumentException("No problems.");
		cont.setProblem(problems.next());
		final DrawPanel pane;
		pane = new DrawPanel(new Dimension(300,300), guesser);
		pane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		add(cont,BorderLayout.NORTH);
		add(pane, BorderLayout.CENTER);
		final JButton button = new JButton(new AbstractAction("Submit") {
			int inputCounter = 0;
			@Override
			public void actionPerformed(ActionEvent e) {
				if(cont.renderer.submitAnswer(pane.getChars())) {
					inputCounter++;
					if(inputCounter == cont.problem.getFullSolution().length()) {
						pane.endProblem();
						try {
							Thread.sleep(500); // TODO : animation
						} catch (InterruptedException e1) {
						}
						if(problems.hasNext()) {
							cont.setProblem(problems.next());
							inputCounter = 0;
						}
					}
				}
				pane.clearStrokes();
				
			}
		});
		add(button,BorderLayout.SOUTH);
	}
	

}
