package ru.andryxx;

import ru.andryxx.logging.PatchLogger;
import ru.andryxx.logging.SystemOutLogger;
import ru.andryxx.mapping.MappingStrategy;
import ru.andryxx.mapping.registry.DefaultMappingRegistry;
import ru.andryxx.mapping.registry.DefaultNamingResolver;
import ru.andryxx.mapping.registry.MappingRegistry;
import ru.andryxx.mapping.registry.NamingResolver;
import ru.andryxx.validation.PatchValidator;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class Patcher<D, E> {
    private final Class<D> dClass;
    private final Class<E> eClass;
    private final NamingResolver namingResolver = new DefaultNamingResolver();
    private MappingRegistry mappingRegistry;
    private PatchLogger patchLogger;
    private PatchValidator<E> patchValidator;

    private final Map<Class<?>, Map<Class<?>, Function<?, ?>>> globalTransformers = new ConcurrentHashMap<>();
    private final Map<String, List<Transformer<?, ?>>> fieldTransformers = new ConcurrentHashMap<>();

    private final Map<String, List<BiPredicate<D, E>>> fieldConditions = new ConcurrentHashMap<>();

    private boolean globalIgnoreNull = false;
    private final HashSet<String> ignoredNullFields = new HashSet<>();
    private final HashSet<String> ignoredFromFields = new HashSet<>();
    private final HashSet<String> ignoredToFields = new HashSet<>();

    private final LinkedList<BiConsumer<D, E>> userPostMappings = new LinkedList<>();

    private Patcher(Class<D> fromClass, Class<E> toClass) {
        this.dClass = fromClass;
        this.eClass = toClass;
    }

    public static <D, E> Patcher<D, E> forType(Class<D> fromClass, Class<E> toClass) {
        return PatcherFactory.createDefault(fromClass, toClass);
    }

    public static <D, E> Patcher<D, E> emptyPatcher(Class<D> fromClass, Class<E> toClass) {
        return new Patcher<>(fromClass, toClass);
    }

    public Patcher<D, E> withFieldMapping(String fromField, String toField) {
        if (mappingRegistry == null) {
            throw new IllegalStateException("Initialize strategy before");
        }
        mappingRegistry.registerFieldMapping(fromField, toField);
        return this;
    }

    public Patcher<D, E> withMappingStrategy(MappingStrategy strategy) {
        mappingRegistry = new DefaultMappingRegistry(namingResolver, strategy);
        return this;
    }

    public <T, R> Patcher<D, E> withTransformer(Transformer<T, R> transformer) {
        globalTransformers
                .computeIfAbsent(transformer.inputType(), _ -> new ConcurrentHashMap<>())
                .put(transformer.outputType(), transformer.function());
        return this;
    }

    public <T, R> Patcher<D, E> withTransformer(Class<T> from, Class<R> to, Function<T, R> fn) {
        return withTransformer(Transformer.of(from, to, fn));
    }

    public <T, R> Patcher<D, E> withTransformer(String field, Transformer<T, R> transformer) {
        fieldTransformers
                .computeIfAbsent(field, _ -> new LinkedList<>())
                .add(transformer);
        return this;
    }

    public <T, R> Patcher<D, E> withTransformer(String field, Class<T> from, Class<R> to, Function<T, R> fn) {
        return withTransformer(field, Transformer.of(from, to, fn));
    }

    public Patcher<D, E> withCondition(String field, BiPredicate<D, E> condition) {
        fieldConditions
                .computeIfAbsent(field, _ -> new LinkedList<>())
                .add(condition);
        return this;
    }

    public Patcher<D, E> ignoreNull(boolean ignore) {
        globalIgnoreNull = ignore;
        return this;
    }

    public Patcher<D, E> ignoreNull() {
        globalIgnoreNull = true;
        return this;
    }

    public Patcher<D, E> ignoreNull(String field, boolean ignore) {
        if (ignore) {
            ignoredNullFields.add(field);
        } else {
            ignoredNullFields.remove(field);
        }
        return this;
    }

    public Patcher<D, E> ignoreNull(String field) {
        return ignoreNull(field, true);
    }

    public Patcher<D, E> ignoreFrom(String field, boolean ignore) {
        if (ignore) {
            ignoredFromFields.add(field);
        } else {
            ignoredFromFields.remove(field);
        }
        return this;
    }

    public Patcher<D, E> ignoreFrom(String field) {
        return ignoreFrom(field, true);
    }

    public Patcher<D, E> ignoreTo(String field, boolean ignore) {
        if (ignore) {
            ignoredToFields.add(field);
        } else {
            ignoredToFields.remove(field);
        }
        return this;
    }

    public Patcher<D, E> ignoreTo(String field) {
        return ignoreTo(field, true);
    }

    public Patcher<D, E> withMap(BiConsumer<D, E> mapping) {
        userPostMappings.add(mapping);
        return this;
    }

    public Patcher<D, E> withLogger(PatchLogger logger) {
        this.patchLogger = logger;
        return this;
    }

    public Patcher<D, E> withLogger(boolean enable) {
        if (enable) {
            patchLogger = new SystemOutLogger();
        } else {
            patchLogger = null;
        }
        return this;
    }

    public Patcher<D, E> withLogger() {
        return withLogger(true);
    }

    public Patcher<D, E> withValidator(PatchValidator<E> validator) {
        this.patchValidator = validator;
        return this;
    }

    //TODO patch(dto, entity)
    //TODO patchOnly
}
