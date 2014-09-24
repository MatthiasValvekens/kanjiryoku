package be.mapariensis.kanjiryoku.net.server.games;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.Constants;
import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.cr.KanjiGuesser;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.GameFlowException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.ServerCommand;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.GameServerInterface;
import be.mapariensis.kanjiryoku.net.server.handlers.AnswerFeedbackHandler;
import be.mapariensis.kanjiryoku.util.Filter;
import be.mapariensis.kanjiryoku.util.GameListener;
import be.mapariensis.kanjiryoku.util.ParsingUtils;

public class TakingTurnsServer implements GameServerInterface {
	private static final Logger log = LoggerFactory.getLogger(TakingTurnsServer.class);
	private static class TurnIterator  {
		private int ix = 0;
		private final List<User> players;
		public TurnIterator(Collection<User> players) {
			this.players = new LinkedList<User>(players);
		}
		// iterate through players circularly
		public synchronized User next() {
			return ix == players.size() ? players.get(ix = 0) : players.get(ix++);
		}
	}
	
	private volatile boolean started = false;
	private final Object submitLock = new Object();
	private volatile User currentPlayer;
	private int problemPosition;
	private Problem currentProblem;
	private TurnIterator ti;
	private final KanjiGuesser guess;
	private final Iterator<Problem> problemSource;
	private final Collection<GameListener> listeners = new LinkedList<GameListener>();
	public TakingTurnsServer(Iterator<Problem> problems, KanjiGuesser guess) {
		this.guess = guess;
		this.problemSource = problems;
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
			try {
				if(msg.argCount() == 3) {
					// submit all strokes
					int width = Integer.parseInt(msg.get(1));
					int height = Integer.parseInt(msg.get(2));
					List<Character> chars =guess.guess(width, height, strokes, Constants.TOLERANCE);
					
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
					if(answer) {
						if(currentProblem.getFullSolution().length() == ++problemPosition) {
							if(problemSource.hasNext()) {
								rh = new NextTurnHandler();
							} else {
								synchronized(listeners) {
									for(GameListener l : listeners) {
										l.finished();
										started = false;
									}
								}
							}
						}
					}
					strokes.clear();
					synchronized(listeners) {
						for(GameListener l : listeners) {
							log.info("Delivering answer "+res);
							l.deliverAnswer(source, answer, res,rh);
							l.clearStrokes(null);
						}
					}					
				}
				// submit one stroke
				// SUBMIT [list_of_dots]
				else if(msg.argCount() == 2) {
					List<Dot> stroke = ParsingUtils.parseDots(msg.get(1));
					synchronized(listeners) {
						for(GameListener l : listeners) {
							l.deliverStroke(source, stroke);
						}
					}
					strokes.add(stroke);
				} else throw new ArgumentCountException(ArgumentCountException.Type.UNEQUAL, ServerCommand.SUBMIT);
			} catch(NumberFormatException ex) {
				throw new ProtocolSyntaxException(ex);
			}
		}
	}

	@Override
	public boolean canPlay(User u) {
		return started && u == currentPlayer;
	}
	@Override
	public boolean running() {
		return started;
	}
	@Override
	public void startGame(Set<User> participants) throws GameFlowException {
		if(started) throw new GameFlowException("Game already running.");
		started = true;
		this.ti = new TurnIterator(participants);
		nextProblem();
	}
	@Override
	public void close() {
		started = false;
		currentPlayer = null;
	}
	@Override
	public void addProblemListener(GameListener p) {
		synchronized(listeners) {
			listeners.add(p);
		}
	}
	@Override
	public void removeProblemListener(GameListener p) {
		synchronized(listeners) {
			listeners.remove(p);
		}
	}
	private void nextProblem() {
		log.info("Next problem");
		problemPosition = 0;
		currentProblem = problemSource.next();
		currentPlayer = ti.next();
		// hence this should be safe
		synchronized(listeners) {
			for(GameListener l : listeners) {
				l.deliverProblem(currentProblem,currentPlayer);
			}
		}
	}
	@Override
	public void clearInput(User submitter) throws GameFlowException {
		if(submitter != null && !submitter.equals(currentPlayer)) throw new GameFlowException("Only the current player can clear the screen.");
		log.info("Clearing input.");
		strokes.clear();
		synchronized(listeners) {
			for(GameListener l : listeners) {
				l.clearStrokes(submitter);
			}
		}
	}
	private class NextTurnHandler extends AnswerFeedbackHandler {
		NextTurnHandler() {
			super(ti.players);
		}
		@Override
		public void afterAnswer() throws ServerException {
			log.info("Received ACK from all users. Proceeding.");
			synchronized(submitLock) {
				nextProblem();
			}
		}

	}
	

}
