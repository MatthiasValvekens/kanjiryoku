package be.mapariensis.kanjiryoku.net.input;

import be.mapariensis.kanjiryoku.gui.GUIBridge;
import be.mapariensis.kanjiryoku.gui.MultipleChoiceInputInterface;
import be.mapariensis.kanjiryoku.model.InputMethod;
import be.mapariensis.kanjiryoku.net.client.ClientCommand;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class MultipleChoiceInputHandlerImpl implements
		MultipleChoiceInputHandler {
	private final GUIBridge bridge;
	private final MultipleChoiceInputInterface choices;
	public MultipleChoiceInputHandlerImpl(GUIBridge bridge,
			MultipleChoiceInputInterface choices) {
		this.bridge = bridge;
		this.choices = choices;
	}
	@Override
	public void receiveMessage(String user, NetworkMessage msg)
			throws ServerCommunicationException {
		ClientCommand.checkArgs(msg, 1);
		try {
			int selectionId = Integer.parseInt(msg.get(0));
			choices.optionSelected(selectionId);			
		} catch(RuntimeException ex) {
			throw new ServerCommunicationException(msg);
		}
	}

	@Override
	public void broadcastClearInput() {
		bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommandList.CLEAR));
	}

	@Override
	public void clearLocalInput() {
		choices.clearSelection();
	}

	@Override
	public InputMethod inputType() {
		return InputMethod.MULTIPLE_CHOICE;
	}

	@Override
	public void submit() {
		bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommandList.SUBMIT));
	}

	@Override
	public void broadcastSelect(int choice) {
		bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommandList.SUBMIT,choice));
	}

}
