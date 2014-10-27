package be.mapariensis.kanjiryoku.net.server.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.Constants;
import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.cr.KanjiGuesser;
import be.mapariensis.kanjiryoku.model.InputMethod;
import be.mapariensis.kanjiryoku.model.MultipleChoiceOptions;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.model.YojiProblem;
import be.mapariensis.kanjiryoku.net.commands.ClientCommandList;
import be.mapariensis.kanjiryoku.net.commands.ParserName;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.GameFlowException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.ClientResponseHandler;
import be.mapariensis.kanjiryoku.net.server.GameServerInterface;
import be.mapariensis.kanjiryoku.net.server.ServerCommand;
import be.mapariensis.kanjiryoku.net.server.Session;
import be.mapariensis.kanjiryoku.net.server.handlers.AnswerFeedbackHandler;
import be.mapariensis.kanjiryoku.problemsets.ProblemOrganizer;
import be.mapariensis.kanjiryoku.util.Filter;
import be.mapariensis.kanjiryoku.util.ParsingUtils;

public class TakingTurnsServer implements GameServerInterface {
	private static final Logger log = LoggerFactory.getLogger(TakingTurnsServer.class);
	private static class Statistics {
		int correct, skipped;
	}
	private static class TurnIterator  {
		private int ix = 0;
		private final List<User> players;
		private final List<Statistics> stats;
		public TurnIterator(Collection<User> players) {
			this.players = new LinkedList<User>(players);
			this.stats = new ArrayList<Statistics>(players.size());
			for(int i = 0; i<players.size();i++) stats.add(new Statistics());
		}
		public Statistics currentUserStats() {
			return stats.get(ix);
		}
		
		// iterate through players circularly
		public synchronized User next() {
			return ++ix == players.size() ? players.get(ix = 0) : players.get(ix);
		}
	}
	
	private volatile boolean gameRunning = false;
	private final boolean enableBatonPass;
	private final Object submitLock = new Object();
	private volatile User currentPlayer;
	private int problemPosition, problemRepetitions, multiProblemChoice = -1;
	private List<String> multiProblemOptions;
	private Problem currentProblem;
	private TurnIterator ti;
	private Session session;
	private final KanjiGuesser guess;
	private final ProblemOrganizer problemSource;
	public TakingTurnsServer(ProblemOrganizer problems, KanjiGuesser guess, boolean batonPass) {
		this.guess = guess;
		this.problemSource = problems;
		this.enableBatonPass = batonPass;
	}
	
	@Override
	public Game getGame() {
		return Game.TAKINGTURNS;
	}
	private final List<List<Dot>> strokes = new LinkedList<List<Dot>>();
	
	@Override
	public void submit(NetworkMessage msg, User source) throws GameFlowException, ProtocolSyntaxException {
		if(!canPlay(source)) throw new GameFlowException("You can't do that now.");
		synchronized(submitLock) {
			switch(currentProblem.getInputMethod()) {
			case HANDWRITTEN:
				handwrittenSubmit(msg,source);
				break;
			case MULTIPLE_CHOICE:
				multipleChoiceSubmit(msg, source);
			}
		}
	}

	
	@Override
	public boolean canPlay(User u) {
		return gameRunning && u == currentPlayer;
	}
	
	@Override
	public boolean running() {
		return gameRunning;
	}
	
	@Override
	public void startGame(Session sess, Set<User> participants) throws GameFlowException, ServerBackendException {
		if(gameRunning) throw new GameFlowException("Game already running.");
		gameRunning = true;
		this.session = sess;
		this.ti = new TurnIterator(participants);
		nextProblem(problemSource.next(true));
	}
	
	@Override
	public void close() {
		gameRunning = false;
		currentPlayer = null;
		guess.close();
	}
	
	private void nextProblem(Problem nextProblem) throws ServerBackendException {
		log.debug("Next problem");
		problemPosition = 0;
		if(currentProblem == nextProblem) problemRepetitions++;
		else problemRepetitions = 0;
		currentProblem = nextProblem;
		currentPlayer = ti.next();
		
		if(currentProblem.getInputMethod() == InputMethod.MULTIPLE_CHOICE) {
			try {
				multiProblemOptions = ((MultipleChoiceOptions)currentProblem).getOptions(0);
			} catch (ClassCastException ex) {
				throw new ServerBackendException(ex);
			}
		}
		// hence this should be safe
		deliverProblem(currentProblem,currentPlayer);
	}
	
	@Override
	public void clearInput(User submitter) throws GameFlowException {
		if(submitter != null && !submitter.equals(currentPlayer)) throw new GameFlowException("Only the current player can clear the screen.");
		log.debug("Clearing input.");
		localClear();
		broadcastClearInput(submitter);
	}
	
	private void localClear() {
		strokes.clear();
		multiProblemChoice = -1;
	}
	private class NextTurnHandler extends AnswerFeedbackHandler {
		final boolean answer, doBatonPass;
		NextTurnHandler(boolean answer, boolean doBatonPass) {
			super(ti.players);
			this.answer = answer;
			this.doBatonPass = doBatonPass;
		}
		
		@Override
		public void afterAnswer() throws ServerException {
			log.debug("All users answered. Moving on.");
			if(doBatonPass || problemSource.hasNext()) {
				synchronized(submitLock) {
					// baton pass ?
					nextProblem(doBatonPass ? currentProblem : problemSource.next(answer));
				}
			} else {
				log.debug("No problems left");
				gameRunning = false;
				TakingTurnsServer.this.finished(stats());
			}
		}

	}

	
	@Override
	public void skipProblem(User submitter) throws GameFlowException {
		if(submitter != null && !submitter.equals(currentPlayer)) throw new GameFlowException("Only the current player can decide to skip a problem.");
		log.debug("Skipping problem.");
		ti.currentUserStats().skipped++;
		boolean batonPass = enableBatonPass && (problemRepetitions<ti.players.size()-1);
		AnswerFeedbackHandler rh = new NextTurnHandler(false,batonPass);
		problemSkipped(submitter,batonPass,rh);
	}
	
	private JSONObject stats() {
		JSONObject res = new JSONObject();
		for(int i = 0;i<ti.players.size();i++) {
			String uname = ti.players.get(i).handle;
			Statistics stats = ti.stats.get(i);
			JSONObject o = new JSONObject();
			o.put("Correct answers", stats.correct);
			o.put("Skipped problems", stats.skipped);
			res.put(uname,o);
		}
		return res;
	}

	
	
	
	// network code (moved from old GameListener)
	private void deliverProblem(Problem p, User to) {
		ParserName parser = p instanceof YojiProblem ? ParserName.YOJI_PARSER : ParserName.KS_PARSER;
		session.broadcastMessage(null,new NetworkMessage(ClientCommandList.PROBLEM,to.handle,parser,p.toString()));
	}

	
	
	private void deliverAnswer(User submitter, boolean wasCorrect,char input, ClientResponseHandler rh) {
		session.broadcastMessage(null,new NetworkMessage(ClientCommandList.ANSWER,submitter.handle,wasCorrect,input, rh != null ? rh.id : -1),rh);
	}

	
	
	private void deliverStroke(User submitter, List<Dot> stroke) {
		String uname = submitter != null ? submitter.handle : Constants.SERVER_HANDLE;
		session.broadcastMessage(submitter,new NetworkMessage(ClientCommandList.INPUT,uname,stroke));
	}

	private void broadcastSelection(User submitter, int selection) {
		session.broadcastMessage(submitter, new NetworkMessage(ClientCommandList.INPUT,submitter.handle,selection));
	}
	
	
	private void broadcastClearInput(User submitter) {
		session.broadcastMessage(submitter, new NetworkMessage(ClientCommandList.CLEAR));
	}

	
	
	private void finished(JSONObject statistics) {
		if(statistics != null) session.broadcastMessage(null, new NetworkMessage(ClientCommandList.STATISTICS,statistics));
		session.destroy();
	}

	
	
	private void problemSkipped(User submitter, boolean batonPass, ClientResponseHandler rh) {
		session.broadcastMessage(null,new NetworkMessage(ClientCommandList.PROBLEMSKIPPED,submitter.handle,rh.id,batonPass),rh);
	}		
	
	private void handwrittenSubmit(NetworkMessage msg, User source) throws ProtocolSyntaxException, GameFlowException {
		try {
			if(msg.argCount() == 3) {
				log.debug("Finalizing input...");
				// submit all strokes
				int width = Integer.parseInt(msg.get(1));
				int height = Integer.parseInt(msg.get(2));
				if(strokes.size() == 0) throw new GameFlowException("No input.");
				List<Character> chars =guess.guess(width, height, strokes);
				log.trace("Retrieved {} characters",chars.size());
				checkAnswer(chars,source);
				localClear();
			}
			// submit one stroke
			// SUBMIT [list_of_dots]
			else if(msg.argCount() == 2) {
				List<Dot> stroke = ParsingUtils.parseDots(msg.get(1));
				strokes.add(stroke);
				deliverStroke(source, stroke);
			} else throw new ArgumentCountException(ArgumentCountException.Type.UNEQUAL, ServerCommand.SUBMIT);
		} catch(NumberFormatException ex) {
			throw new ProtocolSyntaxException(ex);
		}
		
	}
	private void multipleChoiceSubmit(NetworkMessage msg, User source) throws ProtocolSyntaxException, GameFlowException {
		try {
			if(msg.argCount() == 1) {
				if(multiProblemChoice == -1) throw new GameFlowException("No input.");
				log.debug("Submitting multiple choice answer");
				char c = multiProblemOptions.get(multiProblemChoice).charAt(0); // FIXME remove the charAt 0 once I properly generalize the solution model
				checkAnswer(Arrays.asList(c),source);
			} else if(msg.argCount() == 2) {
				int i = Integer.parseInt(msg.get(1));
				if(i<0) throw new ProtocolSyntaxException();
				multiProblemChoice = i;
				broadcastSelection(source, i);
			}
		} catch(NumberFormatException ex) {
			throw new ProtocolSyntaxException(ex);
		}		
	}
	private void checkAnswer(List<Character> chars, User source) {
		boolean answer = currentProblem.checkSolution(chars, problemPosition);
		// build answer packet
		char res;
		if(answer) {
			res = currentProblem.getFullSolution().charAt(problemPosition);
		} else {
			Filter<Character> allowedChars = currentProblem.allowedChars();
			res = '?';
			for(char c : chars) {
				if(allowedChars.accepts(c)) {
					res = c;
					break;
				}
			}
			
		}

		AnswerFeedbackHandler rh = null;
		// move on to next position in problem
		if(answer && currentProblem.getFullSolution().length() == nextPosition()) {
			rh = new NextTurnHandler(true,false);
			ti.currentUserStats().correct++;
		}
		log.debug("Delivering answer "+res);
		deliverAnswer(source, answer, res,rh);
		if(rh == null) broadcastClearInput(null); // do not clear on final input
	}

	private int nextPosition() {
		if(currentProblem.getFullSolution().length() != ++problemPosition && currentProblem.getInputMethod() == InputMethod.MULTIPLE_CHOICE) {
			multiProblemOptions = ((MultipleChoiceOptions)currentProblem).getOptions(problemPosition); // this won't throw an exception, we checked this the first time
			
		}
		return problemPosition;
	}
}
