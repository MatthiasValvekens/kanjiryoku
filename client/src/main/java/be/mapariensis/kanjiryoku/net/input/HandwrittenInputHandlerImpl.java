package be.mapariensis.kanjiryoku.net.input;

import java.util.List;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.gui.DrawingPanelInterface;
import be.mapariensis.kanjiryoku.gui.GUIBridge;
import be.mapariensis.kanjiryoku.model.InputMethod;
import be.mapariensis.kanjiryoku.net.client.ClientCommand;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.util.ParsingUtils;

public class HandwrittenInputHandlerImpl implements HandwrittenInputHandler {
	private final DrawingPanelInterface dpi;
	private final GUIBridge bridge;
	private static enum Command {
		STROKE {
			@Override
			public void execute(NetworkMessage msg, HandwrittenInputHandlerImpl ih)
					throws ServerCommunicationException {
				ClientCommand.checkArgs(msg,2);
				List<Dot> stroke;
				try {
					stroke = ParsingUtils.parseDots(msg.get(1));
				} catch (RuntimeException ex) {
					throw new ServerCommunicationException(msg);
				}
				ih.dpi.drawStroke(stroke);
			}
		}, CLEARSTROKES {
			@Override
			public void execute(NetworkMessage msg, HandwrittenInputHandlerImpl ih) throws ServerCommunicationException {
				ih.clearLocalInput();
			}
		};
		public abstract void execute(NetworkMessage msg, HandwrittenInputHandlerImpl ih) throws ServerCommunicationException;
	}
	public HandwrittenInputHandlerImpl(DrawingPanelInterface dpi, GUIBridge bridge) {
		this.dpi = dpi;
		this.bridge = bridge;
	}

	@Override
	public void receiveMessage(String user, NetworkMessage msg) throws ServerCommunicationException {
		Command c;
		try {
			c = Command.valueOf(msg.get(0));
		} catch(RuntimeException ex) {
			throw new ServerCommunicationException(msg);
		}
		c.execute(msg, this);
	}
	
	@Override
	public void sendStroke(List<Dot> dots) {
		bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommandList.SUBMIT,dots));
	}

	@Override
	public InputMethod inputType() {
		return InputMethod.HANDWRITTEN;
	}

	@Override
	public void broadcastClearInput() {
		bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommandList.CLEAR));
	}

	@Override
	public void clearLocalInput() {
		dpi.clearStrokes();
	}
}
