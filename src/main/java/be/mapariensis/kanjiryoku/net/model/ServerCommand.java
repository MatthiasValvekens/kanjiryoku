package be.mapariensis.kanjiryoku.net.model;

import java.util.ArrayList;
import java.util.List;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.SessionException;
import be.mapariensis.kanjiryoku.net.exceptions.UnsupportedGameException;
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
				if(message.argCount()<2) throw new ArgumentCountException(ArgumentCountException.Type.TOO_FEW,RESPOND);
				client.consumeActiveResponseHandler(message);
			} catch (IndexOutOfBoundsException ex) {
				throw new ProtocolSyntaxException(ex);
			}
		}
		
	}, STARTSESSION {

		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman)
				throws ServerException {
			//enforce arglen
			if(message.argCount() < 2) throw new ArgumentCountException(ArgumentCountException.Type.TOO_FEW, STARTSESSION);
			String gameName = message.get(1);
			Game game;
			try {
				game = Game.valueOf(gameName);
			} catch (RuntimeException ex) {
				throw new UnsupportedGameException(gameName);
			}
			Session sess = sessman.startSession(client, game);
			List<User> users = new ArrayList<User>(message.argCount());
			users.add(client);
			NetworkMessage invite = new NetworkMessage(ClientCommand.INVITE, gameName,String.valueOf(sess.getId()));
			ResponseHandler rh = new SessionInvitationHandler(sess);
			for(int i = 2; i<message.argCount();i++) {
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
	}, INVITE {
		@Override
		public void execute(NetworkMessage message, User client,UserManager userman, SessionManager sessman) throws ServerException {
			if(message.argCount() < 2) throw new ArgumentCountException(ArgumentCountException.Type.TOO_FEW, INVITE);
			synchronized(client.sessionLock()) {
				Session sess;
				if((sess = client.getSession()) == null) throw new SessionException("You are not currently in a session.");
				if(!sess.isMaster(client)) throw new SessionException("Only the session master can invite people.");
				NetworkMessage invite = new NetworkMessage(ClientCommand.INVITE, sess.game.getGame(),String.valueOf(sess.getId()));
				ResponseHandler rh = new SessionInvitationHandler(sess);
				for(int i = 1; i<message.argCount();i++) {
					User u;
					try {
						u = userman.getUser(message.get(i));
						userman.messageUser(u, invite, rh);
					} catch (UserManagementException e) {
						userman.messageUser(client,e.getMessage());
						continue;
					}
				}
			}
			
		}
	},KICK {

		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman) throws ServerException {
			synchronized(client.sessionLock()) {
				Session sess;
				if((sess = client.getSession()) == null) throw new SessionException("You are not currently in a session.");
				if(!sess.isMaster(client)) throw new SessionException("Only the session master can kick people.");
				for(int i = 1; i<message.argCount();i++) {
					User u;
					try {
						u = userman.getUser(message.get(i));
						sess.kickUser(client,u);
					} catch (UserManagementException | SessionException e) {
						userman.messageUser(client,e.getMessage());
						continue;
					}
				}
			}
		}		
	}, WHOAMI {
		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman)
				throws ServerException {
			ArrayList<String> args = new ArrayList<String>(3);
			args.add(client.handle);
			synchronized(client.sessionLock()) {
				Session sess = client.getSession();
				if(sess != null) {
					args.add(String.valueOf(sess.getId()));
					args.add(String.valueOf(sess.isMaster(client)));
					args.add(sess.game.getGame().toString());
				} else {
					args.add("-1");
					args.add("false");
					args.add(Constants.NONE);
				}
			}
			userman.messageUser(client, new NetworkMessage(ClientCommand.RESPOND,args));
			
			
		}
	}, SUBMIT {

		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman)
				throws ServerException {
			synchronized(client.sessionLock()) {
				Session sess = client.getSession();
				if(sess == null || !sess.game.running()) throw new SessionException("No game running.");
				sess.game.submit(message, client);
			}
		}
		
	}, KILLSESSION {

		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman)
				throws ServerException {
			synchronized(client.sessionLock()) {
				Session sess = client.getSession();
				if(sess == null) throw new SessionException("No active session.");
				if(!sess.isMaster(client)) throw new SessionException("Only the session master can kill sessions");
				sessman.destroySession(sess);
			}			
		}
		
	}, STARTGAME {

		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman) throws ServerException {
			synchronized(client.sessionLock()) {
				Session sess = client.getSession();
				if(sess == null) throw new SessionException("No active session.");
				if(!sess.isMaster(client)) throw new SessionException("Only the session master can start the game");
				sess.start();
			}
		}
		
	};
	
	
	public abstract void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman) throws ServerException;
}
