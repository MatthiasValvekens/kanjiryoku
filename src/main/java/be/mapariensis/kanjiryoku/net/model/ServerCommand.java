package be.mapariensis.kanjiryoku.net.model;

import java.util.ArrayList;
import java.util.List;

import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException.Type;
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
			// Register is a special case
		}
		
	}, MESSAGE {

		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman) throws ServerException {
			try {
				String handle = message.get(1);
				NetworkMessage pm = new NetworkMessage(ClientCommand.FROM,client.handle,message.get(2));
				User other = userman.getUser(handle);
				userman.messageUser(other, pm);
			} catch (IndexOutOfBoundsException ex) {
				throw new ProtocolSyntaxException(ex);
			}
		}
	}, SESSIONMESSAGE {
		@Override
		public void execute(NetworkMessage message, User client,
				UserManager userman, SessionManager sessman)
				throws ServerException {
			NetworkMessage msg;
			try {
				msg = new NetworkMessage(ClientCommand.FROM,client.handle,message.get(1));
			} catch (IndexOutOfBoundsException ex) {
				throw new ProtocolSyntaxException(ex);
			}
			synchronized(client.sessionLock()) {
				Session sess;
				if((sess = client.getSession()) == null) throw new SessionException("You are not currently in a session.");
				sess.broadcastMessage(client, msg);
			}
		}
	}, RESPOND {
		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman) throws ServerException {
			if(message.argCount()<2) throw new ArgumentCountException(ArgumentCountException.Type.TOO_FEW,RESPOND);
			client.consumeActiveResponseHandler(message);
		}
		
	}, STARTSESSION {

		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman)
				throws ServerException {
			//enforce arglen
			if(message.argCount() < 2) throw new ArgumentCountException(ArgumentCountException.Type.TOO_FEW, STARTSESSION);
			String gameName = message.get(1).toUpperCase();
			Game game;
			try {
				game = Game.valueOf(gameName);
			} catch (RuntimeException ex) {
				throw new UnsupportedGameException(gameName);
			}
			Session sess = sessman.startSession(client, game);
			userman.humanMessage(client, String.format("Started session of %s.",game.toString()));
			
			List<User> users = new ArrayList<User>(message.argCount());
			users.add(client);
			ClientResponseHandler rh = new SessionInvitationHandler(sess);
			NetworkMessage invite = new NetworkMessage(ClientCommand.INVITE, rh.id, gameName,String.valueOf(sess.getId()), client.handle);
			
			for(int i = 2; i<message.argCount();i++) {
				User u;
				try {
					u = userman.getUser(message.get(i));
					if(u.equals(client)) continue; // self-invites are pretty useless
				} catch (UserManagementException e) {
					userman.messageUser(client,e.protocolMessage);
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
				ClientResponseHandler rh = new SessionInvitationHandler(sess);
				NetworkMessage invite = new NetworkMessage(ClientCommand.INVITE, rh.id,sess.game.getGame(),String.valueOf(sess.getId()), client.handle);
				for(int i = 1; i<message.argCount();i++) {
					User u;
					try {
						u = userman.getUser(message.get(i));
						if(u.equals(client)) continue; // self-invites are pretty useless
						userman.messageUser(u, invite, rh);
					} catch (UserManagementException e) {
						userman.messageUser(client,e.protocolMessage);
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
						userman.messageUser(client,e.protocolMessage);
						continue;
					}
				}
			}
		}		
	}, WHOAMI {
		@Override
		public void execute(NetworkMessage message, User client, UserManager userman, SessionManager sessman)
				throws ServerException {
			if(message.argCount() > 2) throw new ArgumentCountException(Type.TOO_MANY,WHOAMI);
			if(message.argCount() == 1) {
				message = message.concatenate(ResponseHandler.DEFAULT_HANDLER_ID);
			}
			ArrayList<String> args = new ArrayList<String>(4);
			args.add(message.get(1)); // we don't really care what the ID is, as long as it is passed back to the client
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
		
	}, CLEAR {
		@Override
		public void execute(NetworkMessage message, User client,
				UserManager userman, SessionManager sessman)
				throws ServerException {
			synchronized(client.sessionLock()) {
				Session sess = client.getSession();
				if(sess == null || !sess.game.running()) throw new SessionException("No game running.");
				sess.game.clearInput(client);
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
