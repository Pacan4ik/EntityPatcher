package ru.andryxx.mapping.registry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * This implementation does not support nested paths
 */
public class DefaultNamingResolver implements NamingResolver {
    @Override
    public Optional<Method> resolveGetter(Class<?> type, String path) {
        String getterName = "get" + capitalize(path);
        try {
            Method method = type.getMethod(getterName);
            if (isPublicInstance(method) && method.getParameterCount() == 0) {
                return Optional.of(method);
            }
        } catch (NoSuchMethodException ignored) {
        }

        // checks boolean (isBoolean)
        String booleanGetterName = "is" + capitalize(path);
        try {
            Method method = type.getMethod(booleanGetterName);
            if (isPublicInstance(method) &&
                method.getParameterCount() == 0 &&
                method.getReturnType() == boolean.class) {
                return Optional.of(method);
            }
        } catch (NoSuchMethodException ignored) {
        }
        return Optional.empty();
    }

    @Override
    public Optional<Method> resolveSetter(Class<?> type, String path) {
        String setterName = "set" + capitalize(path);
        for (Method method : type.getMethods()) {
            if (method.getName().equals(setterName)
                && isPublicInstance(method)
                && method.getParameterCount() == 1) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Field> resolveField(Class<?> type, String path) {
        try {
            Field field = type.getField(path);
            if (isPublicInstance(field)) {
                return Optional.of(field);
            }
        } catch (NoSuchFieldException ignored) {}
        return Optional.empty();
    }

    @Override
    public Optional<String> resolveEntityPath(Class<?> entityType, String fieldName) {
        return Optional.of(fieldName);
    }

    private boolean isPublicInstance(Method method) {
        int modifiers = method.getModifiers();
        return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
    }

    private boolean isPublicInstance(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
    }

    private String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
