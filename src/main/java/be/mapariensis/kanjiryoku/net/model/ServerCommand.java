package be.mapariensis.kanjiryoku.net.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.mapariensis.kanjiryoku.net.client.ClientCommand;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.UserManagementException;
import be.mapariensis.kanjiryoku.net.server.Session;
import be.mapariensis.kanjiryoku.net.server.SessionManager;
import be.mapariensis.kanjiryoku.net.server.UserManager;
import be.mapariensis.kanjiryoku.net.server.handlers.SessionInvitationHandler;

public enum ServerCommand {
	BYE {
		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman) throws ServerException {
			userman.deregister(client);
		}
	}, REGISTER {

		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman) throws ServerException {
			throw new UnsupportedOperationException();
		}
		
	}, MESSAGE {

		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman) throws ServerException {
			try {
				String handle = message.get(1);
				String pm = String.format("FROM %s %s",client.handle,NetworkMessage.escapedAtom(message.get(2)));
				User other = userman.getUser(handle);
				userman.messageUser(other, pm);
			} catch (IndexOutOfBoundsException ex) {
				throw new ProtocolSyntaxException(ex);
			}
		}
	}, RESPOND {
		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman) throws ServerException {
			try {
				if(message.argCount()<2) throw new ProtocolSyntaxException("Not enough arguments.");
				client.consumeActiveResponseHandler(message);
			} catch (IndexOutOfBoundsException ex) {
				throw new ProtocolSyntaxException(ex);
			}
		}
		
	}, STARTSESSION {

		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman)
				throws ServerException {
			List<User> users = new ArrayList<User>(message.argCount());
			users.add(client);
			Session sess = sessman.startSession(client);
			NetworkMessage invite = new NetworkMessage(ClientCommand.INVITE, Arrays.asList(String.valueOf(sess.getId())));
			ResponseHandler rh = new SessionInvitationHandler(sess);
			for(int i = 1; i<message.argCount();i++) {
				User u;
				try {
					u = userman.getUser(message.get(i));
				} catch (UserManagementException e) {
					userman.messageUser(client,e.getMessage());
					continue;
				}
				users.add(u);
				//dispatch invite
				userman.messageUser(u,invite,rh);
				
			}
			
		}		
	};
	public abstract void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman) throws ServerException;
}
