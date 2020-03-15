package be.mapariensis.kanjiryoku.config;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigFileWatcher extends Thread {
	private static final Logger log = LoggerFactory
			.getLogger(ConfigFileWatcher.class);
	private final WatchService watcher;
	private final Runnable updateTask;
	private final Path configFile;

	public ConfigFileWatcher(Runnable updateTask, Path configFile)
			throws IOException {
		super("FileWatcher");
		this.watcher = FileSystems.getDefault().newWatchService();
		this.updateTask = updateTask;
		this.configFile = configFile;
		setDaemon(true);
	}

	@Override
	public void start() {
		try {
			configFile.getParent().register(watcher, ENTRY_MODIFY);
		} catch (IOException e) {
			log.error("Failed to register configuration file with watcher");
			return;
		}
		super.start();
	}

	public static final int MODIFY_TRESHOLD = 200;

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		long fileTime = -1;
		// listen for config changes
		while (true) {
			WatchKey key = null;
			try {
				key = watcher.take();
			} catch (InterruptedException ignored) {
			}
			if (key == null)
				continue;
			for (WatchEvent<?> evt : key.pollEvents()) {
				if (evt.kind() == OVERFLOW)
					continue;
				WatchEvent<Path> event = (WatchEvent<Path>) evt;
				Path filename = event.context();
				long newFileTime;
				try {
					newFileTime = Files.getLastModifiedTime(filename)
							.toMillis();
				} catch (IOException e) {
					log.error("Failed to read file time.");
					continue;
				}
				if ((newFileTime - fileTime) > MODIFY_TRESHOLD
						&& filename.toString().equals(
								configFile.getFileName().toString())) {
					fileTime = newFileTime;
					try {
						Thread.sleep(MODIFY_TRESHOLD); // sleep for a while,
														// because readAllBytes
														// doesn't like being
														// invoked immediately
					} catch (InterruptedException ignored) {

					}
					updateTask.run();
				}
			}
			boolean valid = key.reset();
			if (!valid)
				break;
		}
	}

}
