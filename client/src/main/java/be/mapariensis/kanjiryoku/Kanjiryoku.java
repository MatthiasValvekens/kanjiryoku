package be.mapariensis.kanjiryoku;

import java.awt.Font;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.FontUIResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigManager;
import be.mapariensis.kanjiryoku.gui.InitWindow;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;

public class Kanjiryoku {
	private static final Logger log = LoggerFactory.getLogger(Kanjiryoku.class);
	private static final String CONFIG_FILE_NAME = "kanjiclient.cfg";

	public static final String FONT_FAMILY = "Tahoma";
	public static final int FONT_SIZE = 15;

	public static void main(String[] args) {
		// Check for config file on command line
		Path configFile;
		switch (args.length) {
		case 0:
			configFile = Paths.get(CONFIG_FILE_NAME);
			break;
		case 1:
			configFile = Paths.get(args[0]);
			break;
		default:
			System.err
					.println(String
							.format("Too many arguments.\n"
									+ "You may optionally pass a configuration file name on the command line. "
									+ "The default is %s.", CONFIG_FILE_NAME));
			return;
		}
		// turn on antialiasing
		System.setProperty("swing.aatext", "true");

		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			log.warn("Nimbus L&F not found");
		}
		// set font
		FontUIResource fr = new javax.swing.plaf.FontUIResource(FONT_FAMILY,
				Font.PLAIN, FONT_SIZE);
		UIManager.getLookAndFeelDefaults().put("defaultFont", fr);
		FontUIResource boldfr = new javax.swing.plaf.FontUIResource(
				FONT_FAMILY, Font.BOLD, FONT_SIZE);
		UIManager.getLookAndFeelDefaults().put("Label.font", boldfr);

		try {
			InitWindow.show(new ConfigManager(configFile));
		} catch (BadConfigurationException | IOException e) {
			log.error("Failed to process configuration.", e);
			return;
		}
	}
}
