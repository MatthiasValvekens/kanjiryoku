package be.mapariensis.kanjiryoku.net.server.handlers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.ClientResponseHandler;


/**
 * This response can be used to run a method after receiving acknowledgement from a group of users.
 * @author Matthias Valvekens
 * @version 1.0
 */
public abstract class AnswerFeedbackHandler extends ClientResponseHandler {
	private static final Logger log = LoggerFactory.getLogger(AnswerFeedbackHandler.class);
	private final Collection<User> allUsers;
	private final Set<User> seen;
	
	public AnswerFeedbackHandler(Collection<User> allUsers) {
		log.debug("Setting up answer feedback handler for {} with id {}",allUsers,id);
		this.allUsers = allUsers;
		this.seen = new HashSet<User>();
	}
	
	// TODO make this more robust
	@Override
	public final synchronized void handle(User user, NetworkMessage msg) throws ServerException {
		if(isFinished()) return;
		// record that the user answered
		seen.add(user);
		log.debug("User {} checked in.",user);
		if(seen.containsAll(allUsers)) {
			finished();
			afterAnswer();
		}
	}
	/**
	 * Excecuted when a RESPOND command is received from all users
	 */
	protected abstract void afterAnswer() throws ServerException;

}
