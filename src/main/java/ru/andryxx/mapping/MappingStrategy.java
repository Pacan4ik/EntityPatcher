package ru.andryxx.mapping;

/**
 * Enum representing different strategies for mapping fields or methods between objects.
 */
public enum MappingStrategy {
    /**
     * Use methods for mapping.
     */
    USE_METHODS,

    /**
     * Use fields for mapping.
     */
    USE_FIELDS,

    /**
     * Use both methods and fields for mapping.
     */
    USE_METHODS_AND_FIELDS
}