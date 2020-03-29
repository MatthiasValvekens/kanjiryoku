package be.mapariensis.kanjiryoku.net.server;

import java.util.Set;

import be.mapariensis.kanjiryoku.net.exceptions.GameFlowException;
import be.mapariensis.kanjiryoku.net.exceptions.ProtocolSyntaxException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerBackendException;
import be.mapariensis.kanjiryoku.net.exceptions.ServerException;
import be.mapariensis.kanjiryoku.net.model.Game;
import be.mapariensis.kanjiryoku.net.model.NetworkMessage;
import be.mapariensis.kanjiryoku.net.model.User;

public interface GameServerInterface extends AutoCloseable {
    Game getGame();

    void submit(NetworkMessage msg, User user) throws GameFlowException,
            ProtocolSyntaxException;

    void update(NetworkMessage msg, User user) throws GameFlowException,
            ProtocolSyntaxException;

    boolean canPlay(User u);

    boolean running();

    void startGame(Session sess, Set<User> participants)
            throws GameFlowException, ServerBackendException;

    @Override
    void close() throws ServerException;

    void clearInput(User submitter) throws GameFlowException;

    void skipProblem(User submitter) throws GameFlowException;
}
