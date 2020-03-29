package be.mapariensis.kanjiryoku.net.exceptions;

public class AuthenticationFailedException extends UserManagementException {

    public AuthenticationFailedException(String username) {
        super(String.format("Failed to authenticate user %s.", username));
    }

}
