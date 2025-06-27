package ru.andryxx.logging;

public class SystemOutLogger implements PatchLogger {

    @Override
    public void logObjInfo(Object from, Object to) {
        System.out.printf("Start patching %s (%s) to %s (%s)%n",
                from.getClass().getSimpleName(), from, to.getClass().getSimpleName(), to);
    }

    @Override
    public void log(String fromField, String toField, Object newValue) {
        System.out.printf("Patched '%s' -> '%s' (new value: %s)%n",
                fromField,
                toField,
                newValue
        );
    }
}
