package ru.andryxx.logging;

@FunctionalInterface
public interface PatchLogger {
    void log(String fromField, String toField, Object newValue);
}
