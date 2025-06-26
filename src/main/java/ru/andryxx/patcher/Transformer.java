package ru.andryxx.patcher;

import java.util.function.Function;

public record Transformer<T, R>(
        Function<T, R> function,
        Class<T> inputType,
        Class<R> outputType
) {
    public static <T, R> Transformer<T, R> of(Class<T> inputType, Class<R> outputType, Function<T, R> function) {
        return new Transformer<>(function, inputType, outputType);
    }
}