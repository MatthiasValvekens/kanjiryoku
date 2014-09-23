package be.mapariensis.kanjiryoku.net.model;

import java.text.ParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.client.ChatInterface;
import be.mapariensis.kanjiryoku.net.client.GameClientInterface;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.util.ParsingUtils;

// TODO : placeholders only
public enum ClientCommand {
	
	SAY {
		@Override
		public void execute(NetworkMessage msg, GameClientInterface gci,
				ChatInterface chat)
				throws ServerCommunicationException {
			if(msg.argCount() != 2) throw new ServerCommunicationException(msg);
			chat.displayServerMessage(msg.get(1));
		}
	},WELCOME {

		@Override
		public void execute(NetworkMessage msg, GameClientInterface gci,
				ChatInterface chat)
				throws ServerCommunicationException {
			if(msg.argCount() != 2) throw new ServerCommunicationException(msg);
			chat.displayServerMessage(String.format("Welcome %s",msg.get(1)));
		}
		
	}, FROM {

		@Override
		public void execute(NetworkMessage msg, GameClientInterface gci,
				ChatInterface chat)
				throws ServerCommunicationException {
			if(msg.argCount()!=3) throw new ServerCommunicationException(msg);
			chat.displayUserMessage(msg.get(1),msg.get(2));
		}
		
		
	}, INVITE {
		@Override
		public void execute(NetworkMessage msg, GameClientInterface gci,
				ChatInterface chat)
				throws ServerCommunicationException {
			if(msg.argCount()!=5) throw new ServerCommunicationException(msg);
			 // TODO : behaviour is undefined when invitations for multiple sessions are sent
			NetworkMessage ifYes = new NetworkMessage(ServerCommand.RESPOND,msg.get(1),Constants.ACCEPTS,msg.get(3));
			NetworkMessage ifNo = new NetworkMessage(ServerCommand.RESPOND,msg.get(1),Constants.REJECTS);
			try {
				chat.yesNoPrompt(String.format("Received an invite from user [%s] for %s. Do you accept?",msg.get(4),Game.valueOf(msg.get(2)).toString()), ifYes, ifNo);
			} catch(IllegalArgumentException ex) {
				throw new ServerCommunicationException(ex);
			}
		}
	}, RESPOND {
		@Override
		public void execute(NetworkMessage msg, GameClientInterface gci,
				ChatInterface chat) throws ClientException {
			if(msg.argCount()<2) throw new ServerCommunicationException(msg);
			gci.consumeActiveResponseHandler(msg);
		}
		
	}, PROBLEM {
		@Override
		public void execute(NetworkMessage msg, GameClientInterface gci,
				ChatInterface chat)
				throws ServerCommunicationException {
			if(msg.argCount() != 3) throw new ServerCommunicationException(msg);
			String name = msg.get(1);
			chat.displayServerMessage(String.format("Server poses a question to %s ",name));
			String problemString = msg.get(2);
			try {
				gci.setProblem(gci.parseProblem(problemString));
			} catch (ParseException e) {
				throw new ServerCommunicationException("Unparseable problem passed: "+problemString);
			}
			// unlock panel as appropriate
			gci.getCanvas().setLock(!name.equals(gci.getUsername()));
		}
	}, ANSWER {
		@Override
		public void execute(NetworkMessage msg, GameClientInterface gci,
				ChatInterface chat)
				throws ServerCommunicationException {
			if(msg.argCount() != 4) throw new ServerCommunicationException(msg);
			String name = msg.get(1);
			boolean wasCorrect = Boolean.valueOf(msg.get(2));
			char inputChar = msg.get(3).charAt(0);
			chat.displayServerMessage(String.format("User %s answered %s, which is %s", name, inputChar, wasCorrect ? "correct" : "unfortunately not the right answer"));
			gci.deliverAnswer(wasCorrect, inputChar);
		}
	}, STROKE {
		@Override
		public void execute(NetworkMessage msg, GameClientInterface gci,
				ChatInterface chat)
				throws ServerCommunicationException {
			if(msg.argCount() != 3) throw new ServerCommunicationException(msg);
			String name = msg.get(1);
			if(name.equals(gci.getUsername())) { //FIXME : get rid of echo
				log.warn("Stroke echo for "+name);
				return;
			}
			log.info("Receiving stroke from "+name);
			List<Dot> stroke = ParsingUtils.parseDots(msg.get(2));
			gci.getCanvas().drawStroke(stroke);
		}
	}, CLEARSTROKES {
		@Override
		public void execute(NetworkMessage msg, GameClientInterface gci, ChatInterface chat)
				throws ServerCommunicationException {
			log.info("Clearing drawing panel");
			gci.getCanvas().clearStrokes();
		}
	};
	private static final Logger log = LoggerFactory.getLogger(ClientCommand.class);
	public abstract void execute(NetworkMessage msg, GameClientInterface gci, ChatInterface chat) throws ClientException;
}
