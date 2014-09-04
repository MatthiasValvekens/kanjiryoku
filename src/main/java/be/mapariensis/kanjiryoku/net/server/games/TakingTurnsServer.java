package be.mapariensis.kanjiryoku.net.server.games;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import be.mapariensis.kanjiryoku.Constants;
import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.cr.KanjiGuesser;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.net.exceptions.ArgumentCountException;
import be.mapariensis.kanjiryoku.net.exceptions.GameFlowException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.ServerCommand;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.GameServerInterface;
import be.mapariensis.kanjiryoku.util.GameListener;

public class TakingTurnsServer implements GameServerInterface {
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
					synchronized(listeners) {
						for(GameListener l : listeners) {
							l.deliverAnswer(source, answer);
						}
					}
					// move on to next position in problem
					if(answer) {
						if(currentProblem.getFullSolution().length() == problemPosition) {
							if(problemSource.hasNext()) {
								nextProblem();
							} else {
								synchronized(listeners) {
									for(GameListener l : listeners) {
										l.finished();
										started = false;
									}
								}
							}
						}
						else problemPosition++;
					}
					strokes.clear();
					synchronized(listeners) {
						for(GameListener l : listeners) {
							l.clearStrokes();
						}
					}
					
				}
				// submit one stroke
				// SUBMIT [list_of_dots]
				else if(msg.argCount() == 2) {
					List<Dot> stroke = parseDots(msg.get(1));
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
	// don't use pure Pattern here, input strings might be very long
	private static List<Dot> parseDots(String in) {
		in = in.replaceAll("(\\[<|>\\])", "");
		String[] dots = in.split(">,\\s*<");
		List<Dot> res = new LinkedList<Dot>();
		for(String dotstring : dots) {
			String[] vals = dotstring.split(",\\s*");
			if(vals.length != 2) throw new NumberFormatException("Too many values in dot");
			res.add(new Dot(Integer.parseInt(vals[0]),Integer.parseInt(vals[1])));
		}
		return res;
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
		currentProblem = problemSource.next();
		currentPlayer = ti.next(); // if this guy's thread barges in right now during submit, it'll still block higher up
		// hence this should be safe
		synchronized(listeners) {
			for(GameListener l : listeners) {
				l.deliverProblem(currentProblem,currentPlayer);
			}
		}
	}
	
	

}
