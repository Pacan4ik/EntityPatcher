package ru.andryxx.patcher.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public record Transformer<T, R>(
        Function<T, R> function,
        Class<T> inputType,
        Class<R> outputType
) {
    private static final Map<String, Transformer<?, ?>> registry = new ConcurrentHashMap<>();

    public static <T, R> Transformer<T, R> of(Class<T> inputType, Class<R> outputType, Function<T, R> function) {
        return new Transformer<>(function, inputType, outputType);
    }

    public static <T, R> void register(String key, Transformer<T, R> transformer) {
        registry.put(key, transformer);
    }

    public static void unregister(String key) {
        registry.remove(key);
    }

    public static Transformer<?, ?> get(String key) {
        return registry.get(key);
    }

    public static boolean contains(String key) {
        return registry.containsKey(key);
    }

    public static void clearRegistry() {
        registry.clear();
    }
}