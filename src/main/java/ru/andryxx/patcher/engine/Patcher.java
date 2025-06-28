package ru.andryxx.patcher.engine;

import ru.andryxx.patcher.exceptions.MappingExecutionException;
import ru.andryxx.patcher.exceptions.ValidationException;
import ru.andryxx.patcher.logging.PatchLogger;
import ru.andryxx.patcher.logging.SystemOutLogger;
import ru.andryxx.patcher.mapping.MappingStrategy;
import ru.andryxx.patcher.mapping.registry.DefaultMappingRegistry;
import ru.andryxx.patcher.mapping.registry.DefaultNamingResolver;
import ru.andryxx.patcher.mapping.registry.MappingRegistry;
import ru.andryxx.patcher.validation.PatchValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public class Patcher<D, E> {
    private final Class<D> dClass;
    private final Class<E> eClass;
    private PatcherEngine<D, E> engine;

    private Patcher(Class<D> fromClass, Class<E> toClass) {
        this.dClass = fromClass;
        this.eClass = toClass;
    }

    public static <D, E> Patcher<D, E> forType(Class<D> fromClass, Class<E> toClass) {
        return PatcherFactory.createDefault(fromClass, toClass);
    }

    public static <D, E> Patcher<D, E> defaultPatcher(Class<D> fromClass, Class<E> toClass) {
        Patcher<D, E> patcher = new Patcher<>(fromClass, toClass);
        patcher.engine = new PatcherEngine<>(patcher.dClass,
                patcher.eClass,
                new DefaultMappingRegistry(new DefaultNamingResolver(), MappingStrategy.USE_METHODS_AND_FIELDS));
        return patcher;
    }

    public Patcher<D, E> useMappingRegistry(MappingRegistry mappingRegistry) {
        engine = new PatcherEngine<>(dClass, eClass, mappingRegistry);
        return this;
    }

    public Patcher<D, E> withFieldMapping(String fromField, String toField) {
        engine.addStaticFieldMapping(fromField, toField);
        return this;
    }

    public Patcher<D, E> withMappingStrategy(MappingStrategy strategy) {
        engine.getMappingRegistry().setStrategy(strategy);
        return this;
    }

    public <T, R> Patcher<D, E> withTransformer(Transformer<T, R> transformer) {
        engine.addGlobalTransformer(transformer);
        return this;
    }

    public <T, R> Patcher<D, E> withTransformer(Class<T> from, Class<R> to, Function<T, R> fn) {
        return withTransformer(Transformer.of(from, to, fn));
    }

    public <T, R> Patcher<D, E> withTransformer(String field, Transformer<T, R> transformer) {
        engine.addFieldTransformer(field, transformer);
        return this;
    }

    public <T, R> Patcher<D, E> withTransformer(String field, Class<T> from, Class<R> to, Function<T, R> fn) {
        return withTransformer(field, Transformer.of(from, to, fn));
    }

    public Patcher<D, E> withCondition(String field, BiPredicate<D, E> condition) {
        engine.addFieldCondition(field, condition);
        return this;
    }

    public Patcher<D, E> ignoreNull(boolean ignore) {
        engine.setGlobalIgnoreNull(ignore);
        return this;
    }

    public Patcher<D, E> ignoreNull() {
        engine.setGlobalIgnoreNull(true);
        return this;
    }

    public Patcher<D, E> ignoreNull(String field, boolean ignore) {
        engine.ignoreNullField(field, ignore);
        return this;
    }

    public Patcher<D, E> ignoreNull(String field) {
        return ignoreNull(field, true);
    }

    public Patcher<D, E> ignoreFrom(String field, boolean ignore) {
        engine.ignoreFromField(field, ignore);
        return this;
    }

    public Patcher<D, E> ignoreFrom(String field) {
        return ignoreFrom(field, true);
    }

    public Patcher<D, E> ignoreTo(String field, boolean ignore) {
        engine.ignoreToField(field, ignore);
        return this;
    }

    public Patcher<D, E> ignoreTo(String field) {
        return ignoreTo(field, true);
    }

    public Patcher<D, E> withMap(BiConsumer<D, E> mapping) {
        engine.addPostMapping(mapping);
        return this;
    }

    public Patcher<D, E> withLogger(PatchLogger logger) {
        engine.setLogger(logger);
        return this;
    }

    public Patcher<D, E> withLogger(boolean enable) {
        if (enable) {
            engine.setLogger(new SystemOutLogger());
        } else {
            engine.setLogger(null);
        }
        return this;
    }

    public Patcher<D, E> withLogger() {
        return withLogger(true);
    }

    public Patcher<D, E> logChange(boolean enable) {
        engine.setGlobalLogChange(enable);
        return this;
    }

    public Patcher<D, E> logChange() {
        return logChange(true);
    }

    public Patcher<D, E> logChange(String field, boolean enable) {
        engine.setFieldLogChange(field, enable);
        return this;
    }

    public Patcher<D, E> logChange(String field) {
        return logChange(field, true);
    }

    public Patcher<D, E> withValidator(PatchValidator<E> validator) {
        engine.setValidator(validator);
        return this;
    }

    public void patch(D fromObject, E toObject) {
        try {
            engine.patch(fromObject, toObject);
        } catch (MappingExecutionException | ValidationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new MappingExecutionException(e);
        }
    }

    public SelectiveMapping patchOnly(String field) {
        var sMapping = new SelectiveMapping();
        sMapping.toFields.add(field);
        return sMapping;
    }

    public class SelectiveMapping {
        List<String> toFields = new ArrayList<>();

        public SelectiveMapping patchOnly(String field) {
            toFields.add(field);
            return this;
        }

        public void apply(D fromObject, E toObject) {
            try {
                engine.patchSelective(toFields, fromObject, toObject);
            } catch (MappingExecutionException | ValidationException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new MappingExecutionException(e);
            }
        }
    }

    public E map(D fromObject) {
        return engine.mapWithDefaultCtor(fromObject);
    }

    public E map(D fromObject, Supplier<? extends E> supplier) {
        return engine.map(fromObject, supplier);
    }
}
