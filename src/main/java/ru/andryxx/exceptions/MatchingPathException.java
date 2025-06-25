package ru.andryxx.exceptions;

public class MatchingPathException extends RuntimeException {
    public MatchingPathException(String message) {
        super(message);
    }

    public MatchingPathException(String message, Throwable cause) {
        super(message, cause);
    }

    public MatchingPathException(Throwable cause) {
        super(cause);
    }
}
