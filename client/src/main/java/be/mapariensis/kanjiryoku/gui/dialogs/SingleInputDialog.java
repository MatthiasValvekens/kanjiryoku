package be.mapariensis.kanjiryoku.gui.dialogs;

import java.awt.Frame;

import javax.swing.JTextField;

import be.mapariensis.kanjiryoku.net.client.ServerUplink;

public abstract class SingleInputDialog extends NetworkedDialog {
    private final JTextField input = new JTextField(20);
    private final boolean requireNotEmpty;

    protected SingleInputDialog(Frame parent, String title, String headerText,
            ServerUplink serv, boolean requireNotEmpty) {
        super(parent, title, headerText, serv);
        getContents().add(input);
        this.requireNotEmpty = requireNotEmpty;
        input.addActionListener(getSubmitAction());
    }

    public String getInput() {
        return input.getText();
    }

    @Override
    protected boolean verifyInput() {
        return !requireNotEmpty || !getInput().isEmpty();
    }
}
