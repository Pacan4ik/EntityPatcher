package ru.andryxx.mapping.registry;

import ru.andryxx.exceptions.MatchingPathException;
import ru.andryxx.mapping.MappingPair;
import ru.andryxx.mapping.MappingStrategy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class DefaultMappingRegistry implements MappingRegistry {
    private record GetterResolution(Class<?> type, Function<Object, Object> func, String name) {
    }

    private record SetterResolution(Class<?> type, BiConsumer<Object, Object> func, String name) {
    }

    private final Map<String, String> userMappings = new ConcurrentHashMap<>();
    private final Map<String, MappingPair> resolvedMappings = new ConcurrentHashMap<>();
    private final NamingResolver namingResolver;
    private final MappingStrategy mappingStrategy;

    public DefaultMappingRegistry(NamingResolver namingResolver, MappingStrategy mappingStrategy) {
        this.namingResolver = namingResolver;
        this.mappingStrategy = mappingStrategy;
    }

    @Override
    public void registerFieldMapping(String fromPath, String toPath) {
        userMappings.put(fromPath, toPath);
    }

    @Override
    public Optional<MappingPair> getFieldMapping(String fromPath) {
        return Optional.ofNullable(resolvedMappings.get(fromPath));
    }

    @Override
    public Set<String> getAllFromObjectFields() {
        return resolvedMappings.keySet();
    }

    @Override
    public void scanEntityMappings(Class<?> fromType, Class<?> toType) throws MatchingPathException {
        // explicit mappings
        for (Map.Entry<String, String> entry : userMappings.entrySet()) {
            String fromField = entry.getKey();
            String toField = entry.getValue();

            GetterResolution getter = resolveGetter(fromType, fromField);
            SetterResolution setter = resolveSetter(toType, toField);

            resolvedMappings.put(fromField, new MappingPair(
                    getter.func(),
                    setter.func(),
                    getter.type(),
                    setter.type(),
                    getter.name(),
                    setter.name()
            ));
        }

        // auto mappings (METHODS)
        if (mappingStrategy == MappingStrategy.USE_METHODS || mappingStrategy == MappingStrategy.USE_METHODS_AND_FIELDS) {
            resolveAutoMappings(
                    toType.getMethods(),
                    method -> uncapitalize(method.getName().substring(3)),
                    this::isSetter,
                    fromType,
                    toType
            );
        }

        // auto mappings (FIELDS)
        if (mappingStrategy == MappingStrategy.USE_FIELDS || mappingStrategy == MappingStrategy.USE_METHODS_AND_FIELDS) {
            resolveAutoMappings(
                    toType.getFields(),
                    Field::getName,
                    f -> true,
                    fromType,
                    toType
            );
        }

    }


    private boolean isSetter(Method method) {
        return method.getName().startsWith("set")
               && method.getParameterCount() == 1
               && Modifier.isPublic(method.getModifiers())
               && !Modifier.isStatic(method.getModifiers());
    }

    private String uncapitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private GetterResolution resolveGetter(Class<?> type, String fieldName) throws MatchingPathException {
        boolean allowMethods = mappingStrategy == MappingStrategy.USE_METHODS
                               || mappingStrategy == MappingStrategy.USE_METHODS_AND_FIELDS;
        boolean allowFields = mappingStrategy == MappingStrategy.USE_FIELDS
                              || mappingStrategy == MappingStrategy.USE_METHODS_AND_FIELDS;

        if (allowMethods) {
            Method mGetter = namingResolver.resolveGetter(type, fieldName).orElse(null);
            if (mGetter != null) {
                Class<?> getterType = mGetter.getReturnType();
                Function<Object, Object> getterFunc = instance -> {
                    try {
                        return mGetter.invoke(instance);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to invoke getter: " + mGetter, e);
                    }
                };
                return new GetterResolution(getterType, getterFunc, mGetter.getName());
            }
        }

        if (allowFields) {
            Field field = namingResolver.resolveField(type, fieldName)
                    .orElseThrow(() -> new MatchingPathException(
                            String.format("Cannot find field%s '%s' in %s",
                                    allowMethods ? " or getter" : "", fieldName, type.getName())
                    ));
            Class<?> getterType = field.getType();
            Function<Object, Object> getterFunc = instance -> {
                try {
                    return field.get(instance);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access field: " + field, e);
                }
            };
            return new GetterResolution(getterType, getterFunc, field.getName());
        }

        throw new MatchingPathException(
                String.format("No valid getter or field '%s' found in %s (strategy: %s)",
                        fieldName, type.getName(), mappingStrategy)
        );
    }


    private SetterResolution resolveSetter(Class<?> type, String fieldName) throws MatchingPathException {
        boolean allowMethods = mappingStrategy == MappingStrategy.USE_METHODS
                               || mappingStrategy == MappingStrategy.USE_METHODS_AND_FIELDS;
        boolean allowFields = mappingStrategy == MappingStrategy.USE_FIELDS
                              || mappingStrategy == MappingStrategy.USE_METHODS_AND_FIELDS;

        if (allowMethods) {
            Method mSetter = namingResolver.resolveSetter(type, fieldName).orElse(null);
            if (mSetter != null) {
                Class<?> setterType = mSetter.getParameterTypes()[0];
                BiConsumer<Object, Object> setterFunc = (instance, value) -> {
                    try {
                        mSetter.invoke(instance, value);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to invoke setter: " + mSetter, e);
                    }
                };
                return new SetterResolution(setterType, setterFunc, mSetter.getName());
            }
        }

        if (allowFields) {
            Field field = namingResolver.resolveField(type, fieldName)
                    .orElseThrow(() -> new MatchingPathException(
                            String.format("Cannot find field %s '%s' in %s",
                                    allowMethods ? " or setter" : "", fieldName, type.getName())
                    ));
            Class<?> setterType = field.getType();
            BiConsumer<Object, Object> setterFunc = (instance, value) -> {
                try {
                    field.set(instance, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access field: " + field, e);
                }
            };
            return new SetterResolution(setterType, setterFunc, field.getName());
        }

        throw new MatchingPathException(
                String.format("No valid setter or field '%s' found in %s (strategy: %s)",
                        fieldName, type.getName(), mappingStrategy)
        );
    }

    private <T> void resolveAutoMappings(
            T[] elements,
            Function<T, String> fieldNameExtractor,
            Predicate<T> filter,
            Class<?> fromType,
            Class<?> toType
    ) {
        for (T element : elements) {
            if (!filter.test(element)) continue;

            String fieldName = fieldNameExtractor.apply(element);
            if (userMappings.containsKey(fieldName) || resolvedMappings.containsKey(fieldName)) continue;

            try {
                GetterResolution getter = resolveGetter(fromType, fieldName);
                SetterResolution setter = resolveSetter(toType, fieldName);
                resolvedMappings.put(fieldName, new MappingPair(
                        getter.func(),
                        setter.func(),
                        getter.type(),
                        setter.type(),
                        getter.name(),
                        setter.name()
                ));
            } catch (MatchingPathException ignored) {
            }
        }
    }

}
