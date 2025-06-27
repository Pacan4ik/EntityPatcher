package ru.andryxx.logging;


public interface PatchLogger {
    void logObjInfo(Object from, Object to);
    void log(String fromField, String toField, Object newValue);
}
