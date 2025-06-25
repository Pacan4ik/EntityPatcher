package ru.andryxx.validation;

import ru.andryxx.exceptions.ValidationException;

@FunctionalInterface
public interface PatchValidator<E> {
    void validate(E entity) throws ValidationException;
}
