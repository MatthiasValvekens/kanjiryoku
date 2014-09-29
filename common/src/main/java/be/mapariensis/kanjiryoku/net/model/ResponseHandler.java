package be.mapariensis.kanjiryoku.net.model;


// TODO implement timeouts
public class ResponseHandler {
	public static final int DEFAULT_HANDLER_ID = -1;
	public final int id;
	private boolean done;
	protected ResponseHandler() {
		// add an ID to the response handler based on the current time
		// this is a "soft" check to prevent the client from accidentally responding to the wrong command
		// if the queueing mechanism goes bananas
		this.id = (int) (System.currentTimeMillis() % 10000);
	}
	protected void finished() {
		done = true;
	}
	public boolean isFinished() {
		return done;
	}
}
