package ru.andryxx.logging;

public class SystemOutLogger implements PatchLogger {

    @Override
    public void log(String field, Object oldValue, Object newValue) {
        System.out.printf("Patched field '%s': %s -> %s%n", field, oldValue.toString(), newValue.toString());
    }
}
