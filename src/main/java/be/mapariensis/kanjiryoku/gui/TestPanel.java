package be.mapariensis.kanjiryoku.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.cr.ZinniaGuesser;

public class TestPanel extends JPanel {
	private static final Logger log = LoggerFactory.getLogger(TestPanel.class);
	public TestPanel() {
		setLayout(new BorderLayout());
		final JTextField text = new JTextField(1);
		final DrawPanel pane;
		try {
			pane = new DrawPanel(new Dimension(300,300), new ZinniaGuesser("data\\writingmodel\\handwriting-ja.model"));
			
		} catch (IOException e) {
			log.error(e.getMessage(),e);
			add(new JLabel("Failed to load handwriting model"));
			return;
		}
		add(text,BorderLayout.NORTH);
		add(pane, BorderLayout.CENTER);
		final JButton button = new JButton(new AbstractAction("Check") {

			@Override
			public void actionPerformed(ActionEvent e) {
				char c = text.getText().charAt(0);
				log.info("Matched {} : {}",c,pane.matches(c));
			}
			
		});
		add(button,BorderLayout.SOUTH);
	}
}
