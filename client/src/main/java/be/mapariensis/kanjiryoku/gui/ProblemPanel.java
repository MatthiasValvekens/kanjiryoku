package be.mapariensis.kanjiryoku.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.gui.utils.TextRendering;
import be.mapariensis.kanjiryoku.model.KakiProblem;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.model.YomiProblem;

public class ProblemPanel extends JPanel{
	private static final Logger log = LoggerFactory.getLogger(ProblemPanel.class);
	private static final Map<Class<? extends Problem>, ProblemRenderer> renderers;
	private Character lastWrongInput = null;
	public ProblemPanel() {
		setPreferredSize(new Dimension(300,120));
	}
	static {
		// TODO move renderer storage to another class
		renderers = new HashMap<Class<? extends Problem>, ProblemRenderer>();
		renderers.put(KakiProblem.class, new KakiRenderer());
		renderers.put(YomiProblem.class, new YomiRenderer());
	}
	public ProblemRenderer getRenderer(Problem problem) {
		for(Class<?> cls : renderers.keySet()) {
			if(cls.isInstance(problem)) {
				return renderers.get(cls);
			}
		}
		return null;
	}
	
	private Problem problem;
	private final List<Character> correctInputs=new ArrayList<Character>();
	private int counter;
	private String actualSolution;
	
	public Problem getProblem() {
		return problem;
	}
	protected String getSolution() {
		return actualSolution;
	}
	protected List<Character> getCorrectInputs() {
		return correctInputs;
	}
	public void setProblem(Problem problem) {
		this.problem = problem;
		lastWrongInput = null;
		correctInputs.clear();
		counter = 0;
		actualSolution = problem != null ? problem.getFullSolution() : null;
		repaint();
	}
	protected int getCounter() {
		return counter;
	}
	private static final int BORDER = 10;
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Rectangle frame = g.getClipBounds();
		if(!(g instanceof Graphics2D)) throw new IllegalArgumentException("Require Graphics2D");
		Graphics2D g2d = (Graphics2D) g.create(frame.x, frame.y, frame.width, frame.height); //normalize graphics to current clip
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0,0,frame.width,frame.height);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setColor(Color.BLACK);
		g2d.translate(BORDER,BORDER);
		if(problem != null) {
			ProblemRenderer r = getRenderer(problem);
			if(r!= null) {
				r.drawProblem(g2d, correctInputs, problem,lastWrongInput);
			} else {
				log.warn("Could not render problem %s",problem.getFullSolution());
				TextRendering.renderWord(g2d, "No renderer available for this problem","",null);
			}
		} else {
			TextRendering.renderWord(g2d, "No problem selected", "",null);
		}
		g2d.dispose();
	}
	public char addCorrectCharacter() {
		lastWrongInput = null;
		if(counter>actualSolution.length()) throw new IllegalStateException("Too many input submissions");
		char added = actualSolution.charAt(counter);
		log.info("Adding new character "+added);
		this.correctInputs.add(added);
		counter++;
		repaint();
		return added;
	}
	
	public void setLastWrongInput(Character c) {
		lastWrongInput = c;
		repaint();
	}
}
