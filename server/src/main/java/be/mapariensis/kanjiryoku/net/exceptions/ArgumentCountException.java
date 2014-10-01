package be.mapariensis.kanjiryoku.net.exceptions;

public class ArgumentCountException extends ProtocolSyntaxException {
	public enum Type {
		TOO_FEW, UNEQUAL, TOO_MANY
	}
	public ArgumentCountException(Type type, Object command) {
		super(buildMessage(type, String.valueOf(command)));
	}
	
	private static String buildMessage(Type type, String command) {
		StringBuilder sb = new StringBuilder();
		switch(type){
		case TOO_FEW:
			sb.append("Too few arguments for command ");
			break;
		case TOO_MANY:
			sb.append("Too many arguments for command ");
			break;
		case UNEQUAL:
			sb.append("Incorrect number of arguments for command ");
		}
		return sb.append(command).toString();
	}
}
