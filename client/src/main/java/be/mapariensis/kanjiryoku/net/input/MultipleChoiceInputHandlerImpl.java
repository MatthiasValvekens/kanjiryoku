package be.mapariensis.kanjiryoku.net.input;

import be.mapariensis.kanjiryoku.gui.MultipleChoiceInputInterface;
import be.mapariensis.kanjiryoku.gui.UIBridge;
import be.mapariensis.kanjiryoku.model.InputMethod;
import be.mapariensis.kanjiryoku.model.MultipleChoiceOptions;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.client.ClientCommand;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerSubmissionException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class MultipleChoiceInputHandlerImpl implements
        MultipleChoiceInputHandler {
    private final UIBridge bridge;
    private final MultipleChoiceInputInterface choices;

    public MultipleChoiceInputHandlerImpl(UIBridge bridge,
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
        } catch (RuntimeException ex) {
            throw new ServerCommunicationException(msg);
        }
    }

    @Override
    public void broadcastClearInput() throws ServerSubmissionException {
        bridge.getUplink().enqueueMessage(
                new NetworkMessage(ServerCommandList.CLEAR));
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
    public void submit() throws ServerSubmissionException {
        bridge.getUplink().enqueueMessage(
                new NetworkMessage(ServerCommandList.SUBMIT));
    }

    @Override
    public void broadcastSelect(int choice) throws ServerSubmissionException {
        bridge.getUplink().enqueueMessage(
                new NetworkMessage(ServerCommandList.UPDATE, choice));
    }

    @Override
    public void prepareProblemPosition(Problem p, int position) {
        choices.setOptions(((MultipleChoiceOptions) p).getOptions(position));
    }

}
