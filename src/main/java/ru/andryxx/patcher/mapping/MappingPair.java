package ru.andryxx.patcher.mapping;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Represents a mapping pair that defines how to map values between two objects.
 *
 * @param getter A function to retrieve a value from the source object.
 * @param setter A bi-consumer to set a value on the target object.
 * @param fromObjectValueType The type of the value in the source object.
 * @param toObjectValueType The type of the value in the target object.
 */
public record MappingPair(
        Function<Object, Object> getter,
        BiConsumer<Object, Object> setter,
        Class<?> fromObjectValueType,
        Class<?> toObjectValueType,
        String fromName,
        String toName,
        String fromFieldName,
        String toFieldName,
        boolean isAutoMapping
) {}

