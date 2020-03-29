package be.mapariensis.kanjiryoku.dict;

import java.io.IOException;

public class DictionaryAccessException extends IOException {

    public DictionaryAccessException() {
        super();
    }

    public DictionaryAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DictionaryAccessException(String message) {
        super(message);
    }

    public DictionaryAccessException(Throwable cause) {
        super(cause);
    }

}
