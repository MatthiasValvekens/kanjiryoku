package be.mapariensis.kanjiryoku.gui.utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.gui.DrawPanel;
import be.mapariensis.kanjiryoku.gui.GUIBridge;
import be.mapariensis.kanjiryoku.gui.TilePanel;
import be.mapariensis.kanjiryoku.model.InputMethod;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.input.InputComponent;
import be.mapariensis.kanjiryoku.net.input.InputHandler;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class InputPanel extends JPanel implements InputHandler {
	private static final Logger log = LoggerFactory.getLogger(InputPanel.class);
	public static final Dimension size = new Dimension(300, 400);
	private final GUIBridge bridge;
	public InputPanel(GUIBridge bridge) {
		this.bridge = bridge;
		setLayout(new BorderLayout());
		setPreferredSize(size);
		dummyPanel.setSize(size);
		add(dummyPanel,BorderLayout.CENTER);
		
	}
	private static final BufferedImage checkmarkImage;
	private static final String CHECKMARK_FILE = "checkmark.png";
	static {
		BufferedImage thing = null;
		try(InputStream is = DrawPanel.class.getClassLoader().getResourceAsStream(CHECKMARK_FILE)) {
				thing = ImageIO.read(is);
		} catch (IOException | IllegalArgumentException e) {
			log.warn("Failed to read checkmark.png",e);
		}
		checkmarkImage = thing;
	}
	private final Map<InputMethod, InputComponent> inputs = new HashMap<>();
	private final JPanel dummyPanel = new JPanel();
	private InputComponent currentComponent;
	private volatile boolean locked;
	private final FadingOverlay fader = new FadingOverlay(this, checkmarkImage);
	public void setInputMethod(InputMethod im) {
		// TODO lock components before swapping?
		if(im == null) {
			if(currentComponent == null) return;
			remove(currentComponent);
			add(dummyPanel,BorderLayout.CENTER);
			currentComponent = null;
			revalidate();
			repaint();			
			return;
		}
		
		InputComponent comp = inputs.get(im);
		if(comp == null){
			switch(im) {
			case HANDWRITTEN:
				comp = new DrawPanel(size,bridge);
				break;
			case MULTIPLE_CHOICE:
				comp = new TilePanel(bridge);
			}
			inputs.put(im,comp);
		}
		if(comp != currentComponent) {
			remove(currentComponent != null ? currentComponent : dummyPanel);
			currentComponent = comp;
			add(currentComponent,BorderLayout.CENTER);
			revalidate();
			repaint();
		}
		currentComponent.setLock(locked);
	}
	
	public void endProblem() {
		if(currentComponent != null) {
			currentComponent.endProblem();
			currentComponent.getInputHandler().clearLocalInput();
			fader.startFade(true);
		}
		else log.warn("No input component available, nothing to do.");
	}
	public void setLock(boolean locked) {
		this.locked = locked;
		if(currentComponent == null) return;
		currentComponent.setLock(locked);
	}
	
	@Override
	public void receiveMessage(String user, NetworkMessage msg)
			throws ServerCommunicationException {
		if(currentComponent != null && currentComponent.getInputHandler() != null)
			currentComponent.getInputHandler().receiveMessage(user, msg);
		else log.warn("Attempted to receive message, but no input handler was available.");
		
	}
	@Override
	public void broadcastClearInput() {
		if(currentComponent != null && currentComponent.getInputHandler() != null)
			currentComponent.getInputHandler().broadcastClearInput();
		else log.warn("Attempted to broadcast clear, but no input handler was available.");
	}
	@Override
	public void clearLocalInput() {
		if(currentComponent != null && currentComponent.getInputHandler() != null)
			currentComponent.getInputHandler().clearLocalInput();
	}
	@Override
	public InputMethod inputType() {
		if(currentComponent != null && currentComponent.getInputHandler() != null)
			return currentComponent.getInputHandler().inputType();
		else return null;
	}
	
	@Override
	public void submit() {
		if(currentComponent != null && currentComponent.getInputHandler() != null)
			currentComponent.getInputHandler().submit();
		else log.warn("Attempted submit, but no input handler was available.");
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		fader.paint(g);
	}
	
}