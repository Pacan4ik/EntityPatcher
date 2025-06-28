package ru.andryxx.patcher.mapping.registry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public interface NamingResolver {
    /**
     * Resolves a method that corresponds to a getter for the specified path in the given type.
     *
     * @param type the class type to search for the getter method
     * @param path the property path to resolve
     * @return an Optional containing the Method if found, or an empty Optional if not found
     */
    Optional<Method> resolveGetter(Class<?> type, String path);

    /**
     * Resolves a method that corresponds to a setter for the specified path in the given type.
     *
     * @param type the class type to search for the setter method
     * @param path the property path to resolve
     * @return an Optional containing the Method if found, or an empty Optional if not found
     */
    Optional<Method> resolveSetter(Class<?> type, String path);

    /**
     * Resolves a field that corresponds to the specified path in the given type.
     *
     * @param type the class type to search for the field
     * @param path the property path to resolve
     * @return an Optional containing the Field if found, or an empty Optional if not found
     */
    Optional<Field> resolveField(Class<?> type, String path);

    /**
     * Resolves the entity path for a given entity type and field name.
     * @param entityType the class type of the entity
     * @param fieldName the name of the field to resolve (e.g., from a DTO)
     * @return an Optional containing the entity path if found, or an empty Optional if not found
     */
    Optional<String> resolveEntityPath(Class<?> entityType, String fieldName);
}
