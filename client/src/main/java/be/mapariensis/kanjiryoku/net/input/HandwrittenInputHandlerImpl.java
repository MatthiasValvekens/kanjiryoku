package be.mapariensis.kanjiryoku.net.input;

import java.util.List;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.gui.DrawingPanelInterface;
import be.mapariensis.kanjiryoku.gui.UIBridge;
import be.mapariensis.kanjiryoku.model.InputMethod;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerSubmissionException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.util.ParsingUtils;

public class HandwrittenInputHandlerImpl implements HandwrittenInputHandler {
    private final DrawingPanelInterface dpi;
    private final UIBridge bridge;

    public HandwrittenInputHandlerImpl(DrawingPanelInterface dpi,
            UIBridge bridge) {
        this.dpi = dpi;
        this.bridge = bridge;
    }

    @Override
    public void receiveMessage(String user, NetworkMessage msg)
            throws ServerCommunicationException {
        List<Dot> stroke;
        try {
            stroke = ParsingUtils.parseDots(msg.get(0));
        } catch (RuntimeException ex) {
            throw new ServerCommunicationException(msg);
        }
        dpi.drawStroke(stroke);
    }

    @Override
    public void sendStroke(List<Dot> dots) throws ServerSubmissionException {
        bridge.getUplink().enqueueMessage(
                new NetworkMessage(ServerCommandList.UPDATE, dots));
    }

    @Override
    public InputMethod inputType() {
        return InputMethod.HANDWRITTEN;
    }

    @Override
    public void broadcastClearInput() throws ServerSubmissionException {
        bridge.getUplink().enqueueMessage(
                new NetworkMessage(ServerCommandList.CLEAR));
    }

    @Override
    public void clearLocalInput() {
        dpi.clearStrokes();
    }

    @Override
    public void submit() throws ServerSubmissionException {
        bridge.getUplink().enqueueMessage(
                new NetworkMessage(ServerCommandList.SUBMIT, dpi.getWidth(),
                        dpi.getHeight()));
    }

    @Override
    public void prepareProblemPosition(Problem p, int position) {
        // nothing to do
    }
}
