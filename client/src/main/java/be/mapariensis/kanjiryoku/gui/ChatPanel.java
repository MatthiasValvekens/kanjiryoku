package be.mapariensis.kanjiryoku.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import be.mapariensis.kanjiryoku.gui.macros.MacroException;
import be.mapariensis.kanjiryoku.gui.macros.UserMacro;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerSubmissionException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.util.History;
import be.mapariensis.kanjiryoku.util.StandardHistoryImpl;

// TODO enforce timestamp ordering
public class ChatPanel extends JPanel {

    private static final String HISTORY_BACK = "history_back";
    private static final String HISTORY_FORWARD = "history_forward";
    public static final int HISTORY_SIZE = 20;
    private static final char macroChar = '!', commandChar = '\\';
    private final History history = new StandardHistoryImpl(HISTORY_SIZE);
    private boolean firstManualCommandSent = false;
    private final JTextField input;

    public ChatPanel(final UIBridge bridge, JComponent chatRenderer) {
        setPreferredSize(new Dimension(600, 600));
        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(chatRenderer);
        scrollPane
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        add(new JLabel("Chat"), BorderLayout.NORTH);
        JPanel controls = new JPanel();
        input = new JTextField(20);
        controls.add(input);

        input.addActionListener(e -> {
            String msg = input.getText();
            history.add(msg);
            if (msg.isEmpty())
                return;
            if (msg.charAt(0) == commandChar && msg.length() > 1) {
                if (!firstManualCommandSent) {
                    bridge.getChat()
                            .displaySystemMessage(
                                    String.format(
                                            "Lines starting with %s are interpreted as server commands.",
                                            commandChar));
                    firstManualCommandSent = true;
                }
                // interpret the rest as a command
                try {
                    bridge.getUplink().enqueueMessage(
                            NetworkMessage.buildArgs(msg.substring(1)));
                } catch (ServerSubmissionException ex) {
                    bridge.getChat().displayErrorMessage(ex);
                }
                input.setText("");
            } else if (msg.charAt(0) == macroChar && msg.length() > 1) {
                // execute user macro
                NetworkMessage nmsg = NetworkMessage.buildArgs(msg
                        .substring(1));
                String macro = nmsg.get(0).toUpperCase();
                try {
                    UserMacro.valueOf(macro).execute(nmsg,
                            (GUIBridge) bridge);
                } catch (IllegalArgumentException ex) {
                    bridge.getChat().displayErrorMessage(
                            ClientServerException.ERROR_UI,
                            String.format("Macro \"%s\" does not exist",
                                    macro));
                } catch (ClassCastException ex) {
                    bridge.getChat().displayErrorMessage(
                            ClientServerException.ERROR_UI,
                            "Bridge is not a GUIBridge.");
                } catch (MacroException ex) {
                    bridge.getChat().displayErrorMessage(ex);
                }
            } else {
                // normal chat message
                NetworkMessage nmsg = new NetworkMessage(
                        ServerCommandList.SESSIONMESSAGE, msg);
                bridge.getChat().displayUserMessage(nmsg.timestamp,
                        bridge.getUplink().getUsername(), msg, true);
                try {
                    bridge.getUplink().enqueueMessage(nmsg);
                } catch (ServerSubmissionException ex) {
                    bridge.getChat().displayErrorMessage(ex);
                }
            }
            input.setText("");
        });
        input.getActionMap().put(HISTORY_BACK,
                new AbstractAction(HISTORY_BACK) {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        input.setText(history.back());
                    }

                });
        input.getActionMap().put(HISTORY_FORWARD,
                new AbstractAction(HISTORY_FORWARD) {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        input.setText(history.forward());
                    }

                });
        input.getInputMap(WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), HISTORY_BACK);
        input.getInputMap(WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), HISTORY_FORWARD);
        add(controls, BorderLayout.SOUTH);
    }

    public void setChatLock(boolean lock) {
        input.setEnabled(!lock);
    }
}