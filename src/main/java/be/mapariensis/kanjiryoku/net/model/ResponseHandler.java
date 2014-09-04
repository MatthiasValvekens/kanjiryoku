package be.mapariensis.kanjiryoku.net.model;

import be.mapariensis.kanjiryoku.net.exceptions.ServerException;



public interface ResponseHandler {
	public void handle(User user, NetworkMessage msg) throws ServerException;
}
