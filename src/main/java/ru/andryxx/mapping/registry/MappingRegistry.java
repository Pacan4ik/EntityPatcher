package ru.andryxx.mapping.registry;

import ru.andryxx.exceptions.MatchingPathException;
import ru.andryxx.mapping.MappingPair;
import ru.andryxx.mapping.MappingStrategy;

import java.util.Optional;
import java.util.Set;

/**
 * Interface for managing field mappings between objects.
 * Provides methods to register, retrieve, and scan mappings.
 */
public interface MappingRegistry {
    /**
     * Sets a strategy for mapping
     *
     * @param strategy The mapping strategy
     */
    void setStrategy(MappingStrategy strategy);

    /**
     * Registers a field mapping between two paths.
     *
     * @param fromPath The source field path.
     * @param toPath The target field path.
     */
    void registerFieldMapping(String fromPath, String toPath);

    /**
     * Retrieves the mapping pair for a given source field path.
     *
     * @param fromPath The source field path.
     * @return An {@link Optional} containing the {@link MappingPair} if found, or empty if no mapping exists.
     */
    Optional<MappingPair> getFieldMapping(String fromPath);

    /**
     * Retrieves all source field paths that have been found.
     *
     * @return A {@link Set} of all founded source field paths.
     */
    Set<String> getAllFromObjectFields();

    /**
     * Scans and registers mappings between fields of two entity types.
     *
     * @param fromType The source entity class.
     * @param toType The target entity class.
     */
    void scanEntityMappings(Class<?> fromType, Class<?> toType) throws MatchingPathException;
}