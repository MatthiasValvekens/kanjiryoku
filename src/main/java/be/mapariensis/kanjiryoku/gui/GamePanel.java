package be.mapariensis.kanjiryoku.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.model.*;
import be.mapariensis.kanjiryoku.net.client.DrawingPanelInterface;
import be.mapariensis.kanjiryoku.net.client.GameClientInterface;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.ServerCommand;
import be.mapariensis.kanjiryoku.net.model.ServerResponseHandler;
import be.mapariensis.kanjiryoku.providers.ProblemParser;

public class GamePanel extends JPanel implements GameClientInterface {
	private static final Logger log = LoggerFactory.getLogger(GamePanel.class);
	private static final Map<Class<? extends Problem>, ProblemRenderer<?>> renderers;
	private final DrawPanel pane;
	private final GUIBridge bridge;
	private final ProblemParser<?> parser;
	private static final Dimension size = new Dimension(300, 400);
	
	private int inputCounter = 0; // keeps track of the current position in the problem for convenience
	
	private class RendererContainer extends JPanel {
		ProblemRenderer<?> renderer;
		Problem problem;
		private void setProblem(Problem problem) {
			this.problem = problem;
			if(this.renderer != null) remove(this.renderer);
			this.renderer = getRenderer(problem); // TODO : make this part more smooth, it's a little hackish now
			add(this.renderer);
		}
	}
	private final RendererContainer cont = new RendererContainer();
	static {
		renderers = new HashMap<Class<? extends Problem>, ProblemRenderer<?>>();
		renderers.put(KakiProblem.class, new KakiRenderer());
		renderers.put(YomiProblem.class, new YomiRenderer());
	}
	@Override
	@SuppressWarnings("unchecked")
	public <T extends Problem> ProblemRenderer<T> getRenderer(T problem) {
		if(problem == null) return null;
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
	public GamePanel(final GUIBridge bridge, ProblemParser<?> parser) {
		this.parser = parser;
		this.bridge = bridge;
		setLayout(new BorderLayout());
		pane = new DrawPanel(size,this);
		pane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		add(cont,BorderLayout.NORTH);
		add(pane, BorderLayout.CENTER);
		final JButton button = new JButton(new AbstractAction("Submit") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				bridge.getUplink().enqueueMessage(ServerCommand.SUBMIT, pane.getWidth(),pane.getHeight());

				pane.clearStrokes();
				
			}
		});
		add(button,BorderLayout.SOUTH);
	}
	@Override
	public void sendStroke(List<Dot> dots) {
		bridge.getUplink().enqueueMessage(ServerCommand.SUBMIT,dots);
	}
	@Override
	public void deliverAnswer(boolean correct, char inputChar) {
		
		if(correct) {
			cont.renderer.addCorrectCharacter();
			inputCounter++;
			if(inputCounter == cont.problem.getFullSolution().length()) {
				try {
					Thread.sleep(500); // TODO : animation
				} catch (InterruptedException e1) {
				}
			}
		} else {
			// TODO : display char in different color if wrong
		}
	}
	@Override
	public Problem parseProblem(String s) throws ParseException {
		return parser.parseProblem(s);
	}
	@Override
	public void setProblem(Problem p) {
		log.info("Setting problem to {}",p.getFullSolution());
		pane.endProblem();
		cont.setProblem(p);
		bridge.pack();
	}
	
	private final List<ServerResponseHandler> activeResponseHandlers = new LinkedList<ServerResponseHandler>();
	
	@Override
	public void consumeActiveResponseHandler(NetworkMessage msg) throws ClientException {
		int passedId;
		try {
			passedId = Integer.valueOf(msg.get(1));
		} catch (IndexOutOfBoundsException ex) {
			// the servercommand class should check this, but an extra safety measure never hurts
			throw new ServerCommunicationException("Too few arguments for RESPOND");
		} catch (RuntimeException ex) {
			throw new ServerCommunicationException(ex);
		}
		if(passedId==-1) {
			bridge.getChat().getDefaultResponseHandler().handle(msg);
			return;
		}
		// there should only be a handful of active rh's at any one time, so linear search is more than good enough
		synchronized(activeResponseHandlers) {
			for(ServerResponseHandler rh : activeResponseHandlers) {
				if(rh.id == passedId) {
					rh.handle(null, msg); // don't mind if this takes long, rh's should be queued anyway
					activeResponseHandlers.remove(rh);
					return;
				}
			}
		}
		throw new ClientException(String.format("No response handler with id %s", passedId),ClientServerException.ERROR_QUEUE);
	}
	@Override
	public String getUsername() {
		return bridge.getUplink().getUsername();
	}
	@Override
	public DrawingPanelInterface getCanvas() {
		return pane;
	}
	@Override
	public void clearInput() {
		bridge.getUplink().enqueueMessage(ServerCommand.CLEAR);
	}

}
