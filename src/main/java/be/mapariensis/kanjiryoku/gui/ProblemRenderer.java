package be.mapariensis.kanjiryoku.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import be.mapariensis.kanjiryoku.model.Problem;

public abstract class ProblemRenderer<T extends Problem> extends JPanel {
	public static final char FILLER_CHAR = '○';
	private T problem;
	private List<Character> correctInputs;
	private int counter;
	private String actualSolution;
	public T getProblem() {
		return problem;
	}
	protected String getSolution() {
		return actualSolution;
	}
	protected List<Character> getCorrectInputs() {
		return correctInputs;
	}
	public void setProblem(T problem) {
		this.problem = problem;
		correctInputs = new ArrayList<Character>();
		counter = 0;
		actualSolution = problem.getFullSolution();
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
		drawProblem(g2d);
		g2d.dispose();
	}
	public abstract void drawProblem(Graphics2D g);
	
	public boolean submitAnswer(List<Character> userInput) {
		if(counter>actualSolution.length()) throw new IllegalStateException("Too many input submissions");
		// check submission
		if(problem.checkSolution(userInput, counter)) {
			this.correctInputs.add(actualSolution.charAt(counter));
			counter++;
			repaint();
			return true;
		} else return false;
	}
}
