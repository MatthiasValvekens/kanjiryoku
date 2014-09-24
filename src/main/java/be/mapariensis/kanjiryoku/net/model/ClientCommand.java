package be.mapariensis.kanjiryoku.net.model;

import java.text.ParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.gui.GUIBridge;
import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.util.ParsingUtils;

// TODO : placeholders only
public enum ClientCommand {
	
	SAY {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			if(msg.argCount() != 2) throw new ServerCommunicationException(msg);
			bridge.getChat().displayServerMessage(msg.get(1));
		}
	},WELCOME {

		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			if(msg.argCount() != 2) throw new ServerCommunicationException(msg);
			bridge.getChat().displayServerMessage(String.format("Welcome %s",msg.get(1)));
		}
		
	}, FROM {

		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			if(msg.argCount()!=3) throw new ServerCommunicationException(msg);
			bridge.getChat().displayUserMessage(msg.get(1),msg.get(2));
		}
		
		
	}, INVITE {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			if(msg.argCount()!=5) throw new ServerCommunicationException(msg);
			 // TODO : behaviour is undefined when invitations for multiple sessions are sent
			NetworkMessage ifYes = new NetworkMessage(ServerCommand.RESPOND,msg.get(1),Constants.ACCEPTS,msg.get(3));
			NetworkMessage ifNo = new NetworkMessage(ServerCommand.RESPOND,msg.get(1),Constants.REJECTS);
			try {
				bridge.getChat().yesNoPrompt(String.format("Received an invite from user [%s] for %s. Do you accept?",msg.get(4),Game.valueOf(msg.get(2)).toString()), ifYes, ifNo);
			} catch(IllegalArgumentException ex) {
				throw new ServerCommunicationException(ex);
			}
		}
	}, RESPOND {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge) throws ClientException {
			if(msg.argCount()<2) throw new ServerCommunicationException(msg);
			bridge.getClient().consumeActiveResponseHandler(msg);
		}
		
	}, PROBLEM {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			if(msg.argCount() != 3) throw new ServerCommunicationException(msg);
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
			if(msg.argCount() != 5) throw new ServerCommunicationException(msg);
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
			bridge.getUplink().enqueueMessage(new NetworkMessage(ServerCommand.RESPOND,responseCode));
		}
	}, STROKE {
		@Override
		public void execute(NetworkMessage msg, GUIBridge bridge)
				throws ServerCommunicationException {
			if(msg.argCount() != 3) throw new ServerCommunicationException(msg);
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
}
