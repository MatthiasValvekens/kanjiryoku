package be.mapariensis.kanjiryoku.net.server.games;

import java.util.Set;

import be.mapariensis.kanjiryoku.cr.KanjiGuesser;
import be.mapariensis.kanjiryoku.net.exceptions.GameFlowException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;
import be.mapariensis.kanjiryoku.net.server.GameServerInterface;

public class TakingTurnsServer implements GameServerInterface {
	private volatile boolean started = false;
	private volatile User currentPlayer;
	private Set<User> players;
	private final KanjiGuesser guess;
	public TakingTurnsServer(KanjiGuesser guess) {
		this.guess = guess;
	}
	@Override
	public Game getGame() {
		return Game.TAKINGTURNS;
	}

	@Override
	public synchronized void submit(NetworkMessage msg, User source) throws GameFlowException {
		if(!canPlay(source)) throw new GameFlowException("You can't do that now.");
		// TODO implement msg->stroke conversion here
	}

	@Override
	public boolean canPlay(User u) {
		return started && u == currentPlayer; // I don't care about atomicity here, except in submit (which is synchronized)
	}
	@Override
	public boolean running() {
		return started;
	}
	@Override
	public void startGame(Set<User> participants) throws GameFlowException {
		if(started) throw new GameFlowException("Game already running.");
		started = true;
		this.players = participants;		
	}
	@Override
	public void close() throws ServerException {
		started = false;
		currentPlayer = null;
	}

}
