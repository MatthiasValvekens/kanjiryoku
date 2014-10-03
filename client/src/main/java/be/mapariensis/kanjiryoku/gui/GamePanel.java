package be.mapariensis.kanjiryoku.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.model.*;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.providers.ProblemParser;

public class GamePanel extends JPanel implements GameClientInterface {
	private static final Logger log = LoggerFactory.getLogger(GamePanel.class);
	
	
	private final DrawPanel pane;
	private final GUIBridge bridge;
	private final ProblemParser<?> parser;
	private final JButton submitButton;
	private static final Dimension size = new Dimension(300, 400);
	 
	private int inputCounter = 0; // keeps track of the current position in the problem for convenience
	private final ProblemPanel cont = new ProblemPanel();

	private volatile boolean locked;
	public GamePanel(final GUIBridge bridge, ProblemParser<?> parser) {
		this.parser = parser;
		this.bridge = bridge;
		setLayout(new BorderLayout());
		pane = new DrawPanel(size,this);
		
		pane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		cont.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		add(cont,BorderLayout.NORTH);
		add(pane, BorderLayout.CENTER);
		submitButton = new JButton(new AbstractAction("Submit") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommandList.SUBMIT, pane.getWidth(),pane.getHeight()));				
			}
		});
		add(submitButton,BorderLayout.SOUTH);
		
		// add double-click listener to problem panel
		// for skipping problems
		
		cont.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent ev) {
				if(!locked && ev.getClickCount() == 2) {
					bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommandList.SKIPPROBLEM));
				}
			}
		});
		setLock(true);
	}
	@Override
	public void sendStroke(List<Dot> dots) {
		bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommandList.SUBMIT,dots));
	}
	@Override
	public void deliverAnswer(boolean correct, char inputChar) {
		
		if(correct) {
			char added = cont.addCorrectCharacter();
			if(inputChar != added) {
				log.warn("Added char differs from input char! %s <> %s",added,inputChar);
			}
			if(++inputCounter == cont.getSolution().length()) {
				pane.endProblem();
			}
		} else {
			cont.setLastWrongInput(inputChar);
		}
	}
	@Override
	public Problem parseProblem(String s) throws ParseException {
		return parser.parseProblem(s);
	}
	@Override
	public void setProblem(Problem p) {
		inputCounter = 0;
		pane.clearStrokes();
		cont.setProblem(p);
	}
	

	@Override
	public String getUsername() {
		return bridge.getUplink().getUsername();
	}
	
	@Override
	public void clearInput() {
		cont.setLastWrongInput(null);
		bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommandList.CLEAR));
	}
	@Override
	public void setLock(boolean locked) {
		log.info("Panel lock set to {}", locked ? "locked" : "released");
		this.locked = locked;
		pane.setLock(locked);
		submitButton.setEnabled(!locked);
	}
	@Override
	public DrawingPanelInterface getCanvas() {
		return pane;
	}
	@Override
	public Problem getProblem() {
		return cont.getProblem();
	}

}
