package be.mapariensis.kanjiryoku.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
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
	private static <T extends Problem> ProblemRenderer<T> getRenderer(Class<T> clazz) {
		return (ProblemRenderer<T>) renderers.get(clazz);
	}
	
	public <T extends Problem> GamePanel(KanjiGuesser guesser, T problem, Class<T> problemClass) {
		setLayout(new BorderLayout());
		final ProblemRenderer<T> renderer = getRenderer(problemClass);
		renderer.setProblem(problem);
		renderer.setPreferredSize(new Dimension(300,120));
		final DrawPanel pane;
		pane = new DrawPanel(new Dimension(300,300), guesser);
		add(renderer,BorderLayout.NORTH);
		add(pane, BorderLayout.CENTER);
		final int maxInput = problem.getFullSolution().length();
		final JButton button = new JButton(new AbstractAction("Submit") {
			int inputCounter = 0;
			@Override
			public void actionPerformed(ActionEvent e) {
				if(renderer.submitAnswer(pane.getChars())) {
					inputCounter++;
					if(inputCounter == maxInput) {
						pane.endProblem();
					}
				}
				pane.clearStrokes();
				
			}
		});
		add(button,BorderLayout.SOUTH);
	}
}
