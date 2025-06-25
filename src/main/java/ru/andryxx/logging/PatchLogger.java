package ru.andryxx.logging;

@FunctionalInterface
public interface PatchLogger {
    void log(String field, Object oldValue, Object newValue);
}
