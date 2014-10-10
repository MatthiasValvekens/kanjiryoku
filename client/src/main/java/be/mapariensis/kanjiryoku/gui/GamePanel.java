package be.mapariensis.kanjiryoku.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.gui.utils.InputPanel;
import be.mapariensis.kanjiryoku.model.*;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.input.InputHandler;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.providers.ProblemParser;

public class GamePanel extends JPanel implements GameClientInterface {
	private static final Logger log = LoggerFactory.getLogger(GamePanel.class);
	
	
	
	private final GUIBridge bridge;
	private final ProblemParser<?> parser;
	private final JButton submitButton;
	
	private int inputCounter = 0; // keeps track of the current position in the problem for convenience
	private final ProblemPanel problemPanel = new ProblemPanel();

	private volatile boolean locked;
	private final InputPanel inputContainer;
	public GamePanel(final GUIBridge bridge, ProblemParser<?> parser) {
		this.parser = parser;
		this.bridge = bridge;
		setLayout(new BorderLayout());
		
		inputContainer = new InputPanel(bridge);
		inputContainer.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		problemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		add(problemPanel,BorderLayout.NORTH);
		add(inputContainer, BorderLayout.CENTER);
		submitButton = new JButton(new AbstractAction("Submit") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				inputContainer.submit();				
			}
		});
		add(submitButton,BorderLayout.SOUTH);
		
		// add double-click listener to problem panel
		// for skipping problems
		
		problemPanel.addMouseListener(new MouseAdapter() {
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
	public void deliverAnswer(boolean correct, char inputChar) {
		
		if(correct) {
			char added = problemPanel.addCorrectCharacter();
			if(inputChar != added) {
				log.warn("Added char differs from input char! %s <> %s",added,inputChar);
			}
			if(++inputCounter == problemPanel.getSolution().length()) {
				inputContainer.endProblem();
			}
		} else {
			problemPanel.setLastWrongInput(inputChar);
		}
	}
	@Override
	public Problem parseProblem(String s) throws ParseException {
		return parser.parseProblem(s);
	}
	@Override
	public void setProblem(final Problem p) {
		inputCounter = 0;
		inputContainer.clearLocalInput();
		problemPanel.setProblem(p);
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				inputContainer.setInputMethod(p != null ? p.getInputMethod() : null);
			}
		});
		
	}
	

	@Override
	public String getUsername() {
		return bridge.getUplink().getUsername();
	}
	
	
	@Override
	public void setLock(boolean locked) {
		log.info("Panel lock set to {}", locked ? "locked" : "released");
		this.locked = locked;
		inputContainer.setLock(locked);
		submitButton.setEnabled(!locked);
	}
	@Override
	public Problem getProblem() {
		return problemPanel.getProblem();
	}
	@Override
	public InputHandler getInputHandler() {
		return inputContainer;
	}

}
