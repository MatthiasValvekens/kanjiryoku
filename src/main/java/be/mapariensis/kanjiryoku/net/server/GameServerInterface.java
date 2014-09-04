package be.mapariensis.kanjiryoku.net.server;

import java.util.Set;

import be.mapariensis.kanjiryoku.net.exceptions.GameFlowException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;

public interface GameServerInterface extends AutoCloseable {
	public Game getGame();
	public void submit(NetworkMessage msg, User user) throws GameFlowException;
	public boolean canPlay(User u);
	public boolean running();
	public void startGame(Set<User> participants) throws GameFlowException;
	@Override
	public void close() throws ServerException;
}
