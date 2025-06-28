package ru.andryxx.patcher.validation;

import ru.andryxx.patcher.exceptions.ValidationException;

@FunctionalInterface
public interface PatchValidator<E> {
    void validate(E entity) throws ValidationException;
}
