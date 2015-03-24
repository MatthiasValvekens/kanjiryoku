package be.mapariensis.kanjiryoku.net.client;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.gui.UIBridge;
import be.mapariensis.kanjiryoku.net.Constants;
import be.mapariensis.kanjiryoku.net.commands.ParserName;
import be.mapariensis.kanjiryoku.net.commands.ServerCommandList;
import be.mapariensis.kanjiryoku.net.exceptions.ClientException;
import be.mapariensis.kanjiryoku.net.exceptions.ClientServerException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerCommunicationException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.secure.SecurityUtils;
import be.mapariensis.kanjiryoku.providers.ProblemParser;

// TODO : placeholders only
public enum ClientCommand {

	SAY {
		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ServerCommunicationException {
			checkArgs(msg, 2);
			bridge.getChat().displayServerMessage(msg.timestamp, msg.get(1));
		}
	},
	STATISTICS {
		@SuppressWarnings("unchecked")
		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ClientException {
			checkArgs(msg, 2);
			StringBuilder sb = new StringBuilder("Statistics:");
			try {
				JSONObject json = new JSONObject(msg.get(1));

				// iterate through users
				Iterator<String> keys = json.keys();
				while (keys.hasNext()) {
					String username = keys.next();
					sb.append("\nUser ").append(username).append(":");
					JSONObject data = json.getJSONObject(username);
					// sort data keys alphabetically
					Set<String> dataKeys = new TreeSet<String>(data.keySet());
					// format scores per category
					for (String key : dataKeys) {
						sb.append("\n\t").append(key).append(": ");
						JSONObject userStats = data.getJSONObject(key);
						Set<String> scoreKeys = new TreeSet<String>(
								userStats.keySet());
						for (String scoreKey : scoreKeys) {
							sb.append("\n\t\t").append(scoreKey).append(": ")
									.append(userStats.get(scoreKey));
						}
					}
				}
			} catch (JSONException ex) {
				throw new ServerCommunicationException(ex);
			}
			bridge.getChat().displayGameMessage(msg.timestamp, sb.toString());
		}
	},
	WELCOME {

		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ServerCommunicationException {
			if (bridge.getUplink().registered())
				throw new ServerCommunicationException(msg);
			checkArgs(msg, 2);
			String username = msg.get(1);
			bridge.setUsername(username);
			bridge.getChat().displayServerMessage(msg.timestamp,
					String.format("Welcome %s", username));
			bridge.getUplink().flagRegisterComplete();
		}

	},
	FROM {

		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ServerCommunicationException {
			checkArgs(msg, 4);
			try {
				bridge.getChat().displayUserMessage(msg.timestamp, msg.get(1),
						msg.get(2), Boolean.valueOf(msg.get(3)));
			} catch (RuntimeException ex) {
				throw new ServerCommunicationException(msg);
			}
		}

	},
	INVITE {
		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ServerCommunicationException {
			checkArgs(msg, 5);
			// TODO : behaviour is undefined when invitations for multiple
			// sessions are sent
			NetworkMessage ifYes = new NetworkMessage(
					ServerCommandList.RESPOND, msg.get(1), Constants.ACCEPTS,
					msg.get(3));
			NetworkMessage ifNo = new NetworkMessage(ServerCommandList.RESPOND,
					msg.get(1), Constants.REJECTS);
			try {
				bridge.getChat()
						.yesNoPrompt(
								String.format(
										"Received an invite from user [%s] for %s. Do you accept?",
										msg.get(4), msg.get(2)), ifYes, ifNo);
			} catch (IllegalArgumentException ex) {
				throw new ServerCommunicationException(ex);
			}
		}
	},
	RESPOND {
		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ClientException {
			if (msg.argCount() < 2)
				throw new ServerCommunicationException(msg);
			bridge.getUplink().consumeActiveResponseHandler(msg);
		}

	},
	PROBLEM {
		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ServerCommunicationException {
			checkArgs(msg, 4);
			String name = msg.get(1);
			bridge.getChat().displayGameMessage(
					msg.timestamp,
					String.format(
							"Question for %s"
									+ (name.equals(bridge.getClient()
											.getUsername()) ? " (You)" : ""),
							name));
			String problemParserName = msg.get(2);
			String problemString = msg.get(3);
			ProblemParser parser;
			try {
				parser = ParserName.valueOf(problemParserName).getParser();
			} catch (IllegalArgumentException ex) {
				throw new ServerCommunicationException(
						"Unknown problem parser: " + problemParserName);
			}
			try {
				bridge.getClient().setProblem(
						parser.parseProblem(problemString));
			} catch (ParseException e) {
				throw new ServerCommunicationException(
						"Unparseable problem passed: " + problemString);
			}
			// unlock panel as appropriate
			bridge.getClient().setLock(
					!name.equals(bridge.getClient().getUsername()));
		}
	},
	ANSWER {
		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ServerCommunicationException {
			checkArgs(msg, 5);
			String name = msg.get(1);
			boolean wasCorrect;
			char inputChar;
			int responseCode;
			try {
				wasCorrect = Boolean.parseBoolean(msg.get(2));
				inputChar = msg.get(3).charAt(0);
				responseCode = Integer.parseInt(msg.get(4));
			} catch (RuntimeException ex) {
				throw new ServerCommunicationException(ex);
			}
			bridge.getChat().displayGameMessage(
					msg.timestamp,
					String.format("User %s answered %s: %s", name, inputChar,
							wasCorrect ? "correct"
									: "unfortunately not the right answer"));
			bridge.getClient().deliverAnswer(wasCorrect, inputChar);
			bridge.getUplink()
					.enqueueMessage(
							new NetworkMessage(ServerCommandList.RESPOND,
									responseCode)); // acknowledge
		}
	},
	PROBLEMSKIPPED {
		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ClientException {
			if (msg.argCount() < 3)
				throw new ServerCommunicationException(msg);
			int responseCode;
			boolean batonPass;
			try {
				responseCode = Integer.parseInt(msg.get(2));
				batonPass = msg.argCount() > 3
						&& Boolean.parseBoolean(msg.get(3));
			} catch (RuntimeException ex) {
				throw new ServerCommunicationException(ex);
			}
			bridge.getChat().displayGameMessage(msg.timestamp,
					String.format("User %s failed the problem.", msg.get(1)));
			if (!batonPass)
				bridge.getChat().displayGameMessage(
						msg.timestamp,
						String.format("The full solution was %s.", bridge
								.getClient().getProblem().getFullSolution()));
			bridge.getUplink()
					.enqueueMessage(
							new NetworkMessage(ServerCommandList.RESPOND,
									responseCode));
		}
	},
	RESETUI {

		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ClientException {
			log.info("Resetting UI");
			bridge.getClient().getInputHandler().clearLocalInput();
			bridge.getClient().setLock(true);
			bridge.getClient().setProblem(null);
		}

	},
	VERSION {

		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ClientException {
			checkArgs(msg, 3);
			boolean incompatible;
			int major, minor;
			try {
				major = Integer.parseInt(msg.get(1));
				minor = Integer.parseInt(msg.get(2));
				incompatible = (major != Constants.protocolMajorVersion || minor != Constants.protocolMinorVersion);
			} catch (RuntimeException ex) {
				// assume 0.1
				incompatible = true;
				major = 0;
				minor = 1;
			}
			if (incompatible) {
				bridge.getChat()
						.displaySystemMessage(
								String.format(
										"Warning:\nthe server's reported protocol version (%s.%s)\n"
												+ "does not match the client's (%s.%s)\n"
												+ "Unexpected behaviour may arise.",
										major, minor,
										Constants.protocolMajorVersion,
										Constants.protocolMinorVersion));
			}
		}

	},
	CLEAR {
		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ClientException {
			bridge.getClient().getInputHandler().clearLocalInput();
		}
	},
	INPUT {

		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ClientException {
			if (msg.argCount() < 2)
				throw new ServerCommunicationException(msg);
			bridge.getClient().getInputHandler()
					.receiveMessage(msg.get(1), msg.truncate(2));
		}

	},
	HELLO {
		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ClientException {
			log.debug("Server says hello.");
			checkArgs(msg, 2);
			bridge.getChat().displayServerMessage(msg.timestamp, msg.get(1));
			bridge.getUplink().requestMode();
		}
	},
	SETMODE {
		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ClientException {
			checkArgs(msg, 2);
			String mode = msg.get(1);
			if (Constants.MODE_PLAIN.equals(mode))
				bridge.getUplink().plaintextMode();
			else if (Constants.MODE_TLS.equals(mode))
				bridge.getUplink().sslMode();
			else
				throw new ServerCommunicationException(msg);
		}
	},
	AUTH {

		@Override
		public void execute(NetworkMessage msg, UIBridge bridge)
				throws ClientException {
			checkArgs(msg, 2);
			if (!bridge.getUplink().registered()) {
				String serverSalt = msg.get(1);
				// this should be the hash the server retrieved from the
				// database
				String pw = bridge.promptPassword();
				if (pw == null) {
					bridge.close();
					return;
				}
				String dbHash = BCrypt.hashpw(pw, serverSalt);
				// generate client salt and SHA256 hash

				// doesn't have to be a bcrypt salt
				String clientSalt = Long
						.toString(new SecureRandom().nextLong());
				String hash;
				try {
					hash = SecurityUtils.sha256(dbHash + clientSalt);
				} catch (NoSuchAlgorithmException e) {
					throw new ClientException(e,
							ClientServerException.ERROR_NOT_SUPPORTED);
				}
				NetworkMessage reply = new NetworkMessage(
						ServerCommandList.AUTH, hash, clientSalt);
				bridge.getUplink().enqueueMessage(reply);
			}

		}

	};

	private static final Logger log = LoggerFactory
			.getLogger(ClientCommand.class);

	public abstract void execute(NetworkMessage msg, UIBridge bridge)
			throws ClientException;

	public static void checkArgs(NetworkMessage msg, int args)
			throws ServerCommunicationException {
		if (msg.argCount() != args)
			throw new ServerCommunicationException(msg);
	}
}
