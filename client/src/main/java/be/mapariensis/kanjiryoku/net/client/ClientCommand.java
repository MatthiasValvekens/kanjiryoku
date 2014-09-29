package be.mapariensis.kanjiryoku.net.client;

import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.gui.GUIBridge;
import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.util.ParsingUtils;

// TODO : placeholders only
public enum ClientCommand {
	
	SAY {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			checkArgs(msg,2);
			bridge.getChat().displayServerMessage(msg.get(1));
		}
	}, STATISTICS {
		@SuppressWarnings("unchecked")
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ClientException {
			checkArgs(msg,2);
			StringBuilder sb = new StringBuilder("Statistics:");
			try {
				JSONObject json = new JSONObject(msg.get(1));
				
				// iterate through users
				Iterator<String> keys = json.keys();
				while(keys.hasNext()) {
					String username = keys.next();
					sb.append("\nUser ").append(username).append(":");
					JSONObject data = json.getJSONObject(username);
					//sort data keys alphabetically
					Set<String> dataKeys = new TreeSet<String>(data.keySet());
					for(String key : dataKeys)
						sb.append("\n\t").append(key).append(": ").append(data.get(key));
				}
			} catch(JSONException ex) {
				throw new ServerCommunicationException(ex);
			}
			bridge.getChat().displayServerMessage(sb.toString());
		}
	}, WELCOME {

		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			checkArgs(msg,2);
			bridge.getChat().displayServerMessage(String.format("Welcome %s",msg.get(1)));
		}
		
	}, FROM {

		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			checkArgs(msg,3);
			bridge.getChat().displayUserMessage(msg.get(1),msg.get(2));
		}
		
		
	}, INVITE {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			checkArgs(msg,5);
			 // TODO : behaviour is undefined when invitations for multiple sessions are sent
			NetworkMessage ifYes = new NetworkMessage(ServerCommandList.RESPOND,msg.get(1),Constants.ACCEPTS,msg.get(3));
			NetworkMessage ifNo = new NetworkMessage(ServerCommandList.RESPOND,msg.get(1),Constants.REJECTS);
			try {
				bridge.getChat().yesNoPrompt(String.format("Received an invite from user [%s] for %s. Do you accept?",msg.get(4),msg.get(2)), ifYes, ifNo);
			} catch(IllegalArgumentException ex) {
				throw new ServerCommunicationException(ex);
			}
		}
	}, RESPOND {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge) throws ClientException {
			if(msg.argCount()<2) throw new ServerCommunicationException(msg);
			bridge.getUplink().consumeActiveResponseHandler(msg);
		}
		
	}, PROBLEM {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			checkArgs(msg,3);
			String name = msg.get(1);
			bridge.getChat().displayServerMessage(String.format("Question for %s" + (name.equals(bridge.getClient().getUsername()) ? " (You)" : ""),name));
			String problemString = msg.get(2);
			try {
				bridge.getClient().setProblem(bridge.getClient().parseProblem(problemString));
			} catch (ParseException e) {
				throw new ServerCommunicationException("Unparseable problem passed: "+problemString);
			}
			// unlock panel as appropriate
			bridge.getClient().setLock(!name.equals(bridge.getClient().getUsername()));
		}
	}, ANSWER {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			checkArgs(msg,5);
			String name = msg.get(1);
			boolean wasCorrect;
			char inputChar;
			int responseCode;
			try {
				wasCorrect = Boolean.parseBoolean(msg.get(2));
				inputChar = msg.get(3).charAt(0);
				responseCode = Integer.parseInt(msg.get(4));
			} catch(RuntimeException ex) {
				throw new ServerCommunicationException(ex);
			}
			bridge.getChat().displayServerMessage(String.format("User %s answered %s, which is %s", name, inputChar, wasCorrect ? "correct" : "unfortunately not the right answer"));
			bridge.getClient().deliverAnswer(wasCorrect, inputChar);
			bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommandList.RESPOND,responseCode)); // acknowledge 
		}
	}, PROBLEMSKIPPED {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ClientException {
			checkArgs(msg,3);
			int responseCode;
			try {
				responseCode = Integer.parseInt(msg.get(2));
			} catch(RuntimeException ex) {
				throw new ServerCommunicationException(ex);
			}
			bridge.getChat().displayServerMessage(String.format("User %s skipped the problem. The full solution was %s.",msg.get(1),bridge.getClient().getProblem().getFullSolution()));
			bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommandList.RESPOND,responseCode));
		}
	},STROKE {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			checkArgs(msg,3);
			String name = msg.get(1);
			if(name.equals(bridge.getClient().getUsername())) { //FIXME : get rid of echo
				log.warn("Stroke echo for "+name);
				return;
			}
			List<Dot> stroke = ParsingUtils.parseDots(msg.get(2));
			bridge.getClient().getCanvas().drawStroke(stroke);
		}
	}, CLEARSTROKES {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			log.info("Clearing drawing panel");
			bridge.getClient().getCanvas().clearStrokes();
		}
	}, RESETUI {

		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ClientException {
			log.info("Resetting UI");
			bridge.getClient().getCanvas().clearStrokes();
			bridge.getClient().setLock(true);
			bridge.getClient().setProblem(null);
		}
		
	};
	private static final Logger log = LoggerFactory.getLogger(ClientCommand.class);
	public abstract void execute(NetworkMessage msg, GUIBridge bridge) throws ClientException;
	private static void checkArgs(NetworkMessage msg, int args) throws ServerCommunicationException {
		if(msg.argCount() != args) throw new ServerCommunicationException(msg);
	}
}
