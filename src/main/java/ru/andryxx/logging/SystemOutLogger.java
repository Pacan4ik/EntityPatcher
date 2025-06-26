package ru.andryxx.logging;

public class SystemOutLogger implements PatchLogger {

    @Override
    public void log(String fromField, String toField, Object newValue) {
        System.out.printf("Patched '%s' -> '%s' (new value: %s)%n", fromField, toField, newValue.toString());
    }
}
