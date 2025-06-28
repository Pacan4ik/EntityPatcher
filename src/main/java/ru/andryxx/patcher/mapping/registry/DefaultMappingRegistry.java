package ru.andryxx.patcher.mapping.registry;

import ru.andryxx.patcher.exceptions.MatchingPathException;
import ru.andryxx.patcher.mapping.MappingPair;
import ru.andryxx.patcher.mapping.MappingStrategy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
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

    private final Map<String, Set<String>> userMappings = new ConcurrentHashMap<>();
    private final Map<String, Set<MappingPair>> resolvedMappings = new ConcurrentHashMap<>();
    private final NamingResolver namingResolver;
    private MappingStrategy mappingStrategy;

    public DefaultMappingRegistry(NamingResolver namingResolver, MappingStrategy mappingStrategy) {
        this.namingResolver = namingResolver;
        this.mappingStrategy = mappingStrategy;
    }

    @Override
    public void setStrategy(MappingStrategy strategy) {
        this.mappingStrategy = strategy;
    }

    @Override
    public void registerFieldMapping(String fromPath, String toPath) {
        userMappings.forEach((key, targets) -> targets.remove(toPath));
        userMappings.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        userMappings.computeIfAbsent(fromPath, _ -> new HashSet<>()).add(toPath);
    }

    @Override
    public Set<MappingPair> getFieldMappings(String fromPath) {
        return resolvedMappings.getOrDefault(fromPath, Set.of());
    }

    @Override
    public Set<String> getAllResolvedFromObject() {
        return resolvedMappings.keySet();
    }

    @Override
    public void scanEntityMappings(Class<?> fromType, Class<?> toType) throws MatchingPathException {
        resolvedMappings.clear();
        // explicit mappings
        for (Map.Entry<String, Set<String>> entry : userMappings.entrySet()) {
            String fromField = entry.getKey();
            for (String toField : entry.getValue()) {
                resolve(resolveGetter(fromType, fromField), resolveSetter(toType, toField), fromField, toField, false);
            }
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
            if (userMappings.containsKey(fieldName)
                || userMappings.values().stream().anyMatch(s -> s.contains(fieldName))
                || resolvedMappings.containsKey(fieldName)
                || resolvedMappings.values().stream().anyMatch(s -> s.stream()
                    .anyMatch(m -> m.toFieldName().equals(fieldName)))) {
                continue;
                // do smth with this statement
            }
            try {
                resolve(resolveGetter(fromType, fieldName), resolveSetter(toType, fieldName), fieldName, fieldName, true);
            } catch (MatchingPathException ignored) {
            }
        }
    }

    private void resolve(GetterResolution fromType, SetterResolution toType, String fromName, String toName, boolean isAutoMapping) {
        resolvedMappings.computeIfAbsent(fromName, _ -> new HashSet<>())
                .add(new MappingPair(
                        fromType.func(),
                        toType.func(),
                        fromType.type(),
                        toType.type(),
                        fromType.name(),
                        toType.name(),
                        fromName,
                        toName,
                        isAutoMapping
                ));
    }

}
