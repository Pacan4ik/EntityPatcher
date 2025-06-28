package ru.andryxx.patcher.exceptions;

public class MappingExecutionException extends RuntimeException {
    public MappingExecutionException(String message) {
        super(message);
    }

    public MappingExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public MappingExecutionException(Throwable cause) {
        super(cause);
    }
}
