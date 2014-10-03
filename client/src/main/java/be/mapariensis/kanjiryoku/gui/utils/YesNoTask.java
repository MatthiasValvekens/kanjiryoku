package be.mapariensis.kanjiryoku.gui.utils;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import be.mapariensis.kanjiryoku.net.client.ServerUplink;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;

public class YesNoTask implements Runnable {
	private final String question;
	private final NetworkMessage ifNo, ifYes;
	private final JComponent parent;
	private final ServerUplink uplink;
	public YesNoTask(JComponent parent, ServerUplink uplink, String question, NetworkMessage ifYes,
			NetworkMessage ifNo) {
		this.question = question;
		this.ifNo = ifNo;
		this.ifYes = ifYes;
		this.parent = parent;
		this.uplink = uplink;
	}

	@Override
	public void run() {
		int res = JOptionPane.showConfirmDialog(parent, question,"Server prompt",JOptionPane.YES_NO_OPTION);
		uplink.enqueueMessage(res == JOptionPane.YES_OPTION ? ifYes : ifNo);
	}
	
}
