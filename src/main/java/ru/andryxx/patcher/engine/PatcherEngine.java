package ru.andryxx.patcher.engine;

import ru.andryxx.patcher.exceptions.MappingExecutionException;
import ru.andryxx.patcher.exceptions.ValidationException;
import ru.andryxx.patcher.logging.PatchLogger;
import ru.andryxx.patcher.mapping.MappingPair;
import ru.andryxx.patcher.mapping.registry.MappingRegistry;
import ru.andryxx.patcher.validation.PatchValidator;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

class PatcherEngine<D, E> {
    protected static final class PatchApplier<D, E> {
        private final Function<Object, Object> getter;
        private final BiConsumer<Object, Object> setter;
        private final Function<Object, Object> function;

        private PatchApplier(Function<Object, Object> getter,
                             BiConsumer<Object, Object> setter,
                             Function<Object, Object> function) {
            this.getter = getter;
            this.setter = setter;
            this.function = Objects.requireNonNullElse(function, Function.identity());
        }

        public Object apply(D d, E e) {
            Object value = getter.apply(d);
            Object transformedValue = function.apply(value);
            setter.accept(e, transformedValue);
            return transformedValue;
        }

        public Function<Object, Object> getter() {
            return getter;
        }

        public BiConsumer<Object, Object> setter() {
            return setter;
        }

        public Function<Object, Object> function() {
            return function;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (PatchApplier) obj;
            return Objects.equals(this.getter, that.getter) &&
                   Objects.equals(this.setter, that.setter) &&
                   Objects.equals(this.function, that.function);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getter, setter, function);
        }

        @Override
        public String toString() {
            return "PatchApplier[" +
                   "getter=" + getter + ", " +
                   "setter=" + setter + ", " +
                   "function=" + function + ']';
        }

    }

    protected record PatchStep<D, E>(
            MappingPair mapping,
            PatchApplier<D, E> applier
    ) {
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_BOXED = Map.ofEntries(
            Map.entry(boolean.class, Boolean.class),
            Map.entry(byte.class, Byte.class),
            Map.entry(char.class, Character.class),
            Map.entry(double.class, Double.class),
            Map.entry(float.class, Float.class),
            Map.entry(int.class, Integer.class),
            Map.entry(long.class, Long.class),
            Map.entry(short.class, Short.class),
            Map.entry(void.class, Void.class)
    );

    private final Class<D> dClass;
    private final Class<E> eClass;
    private final PatchContext<D, E> context = new PatchContext<>();

    private List<MappingPair> mappingPairs = new LinkedList<>();

    // is a reload necessary (when the user made changes after mapping)
    // transformers, conditions
    private boolean isContextValid = false;
    // mappings in MappingRegistry (more strictly, if false then context invalid)
    private boolean isMappingsValid = false;

    private final List<PatchStep<D, E>> patchSteps = new LinkedList<>();

    private final AnnotationProcessor annotationProcessor = new AnnotationProcessor();

    public PatcherEngine(Class<D> dClass, Class<E> eClass, MappingRegistry mappingRegistry) {
        this.dClass = dClass;
        this.eClass = eClass;
        context.setMappingRegistry(mappingRegistry);
    }

    public MappingRegistry getMappingRegistry() {
        isContextValid = false;
        isMappingsValid = false;
        return context.getMappingRegistry();
    }

    public void addStaticFieldMapping(String dField, String eField) {
        isContextValid = false;
        isMappingsValid = false;
        context.getMappingRegistry().registerFieldMapping(dField, eField);
    }

    public <T, R> void addGlobalTransformer(Transformer<T, R> transformer) {
        isContextValid = false;
        context.getGlobalTransformers()
                .computeIfAbsent(transformer.inputType(), _ -> new ConcurrentHashMap<>())
                .put(transformer.outputType(), transformer.function());
    }

    public <T, R> void addFieldTransformer(String field, Transformer<T, R> transformer) {
        isContextValid = false;
        context.getFieldTransformers()
                .computeIfAbsent(field, _ -> new LinkedList<>())
                .add(transformer);
    }

    public void addFieldCondition(String field, BiPredicate<D, E> condition) {
        isContextValid = false;
        context.getFieldConditions()
                .computeIfAbsent(field, _ -> new LinkedList<>())
                .add(condition);
    }

    public void setGlobalIgnoreNull(boolean ignore) {
        isContextValid = context.isGlobalIgnoreNull() == ignore;
        context.setGlobalIgnoreNull(ignore);
    }

    public void ignoreNullField(String field, boolean ignore) {
        if (ignore) {
            isContextValid = !context.getIgnoredNullFields().add(field);
        } else {
            isContextValid = !context.getIgnoredNullFields().remove(field);
        }
    }

    public void ignoreFromField(String field, boolean ignore) {
        if (ignore) {
            isContextValid = !context.getIgnoredFromFields().add(field);
        } else {
            isContextValid = !context.getIgnoredFromFields().remove(field);
        }
    }

    public void ignoreToField(String field, boolean ignore) {
        if (ignore) {
            isContextValid = !context.getIgnoredToFields().add(field);
        } else {
            isContextValid = !context.getIgnoredToFields().remove(field);
        }
    }

    public void addPostMapping(BiConsumer<D, E> mapping) {
        context.getUserPostMappings().add(mapping);
    }

    public void setLogger(PatchLogger patchLogger) {
        context.setPatchLogger(patchLogger);
    }

    public void setGlobalLogChange(boolean log) {
        context.setGlobalLogChange(log);
    }

    public void setFieldLogChange(String field, boolean log) {
        context.getLogChangeFields().put(field, log);
    }

    public void setValidator(PatchValidator<E> validator) {
        context.setPatchValidator(validator);
    }

    public void patch(D dObject, E eObject) throws ValidationException, MappingExecutionException {
        List<MappingPair> mappings = isMappingsValid ? mappingPairs : getMappings();
        if (!isMappingsValid || !isContextValid) {
            patchSteps.clear();
            mappingPairs = mappings;
            mappings = filterConditions(filterNull(filterIgnored(mappings.stream()), dObject), dObject, eObject)
                    .toList();
            var globalTransformers = context.getGlobalTransformers();
            var fieldTransformers = context.getFieldTransformers();
            patchSteps.addAll(getPatchSteps(mappings, fieldTransformers, globalTransformers));
            isMappingsValid = true;
            isContextValid = true;
        }

        before(dObject, eObject);
        processPatchSteps(dObject, eObject, patchSteps);
        processPostMappings(dObject, eObject);
        PatchValidator<E> validator = context.getPatchValidator();
        if (validator != null) {
            validator.validate(eObject);
        }

    }

    public void patchSelective(Collection<String> eFields, D dObject, E eObject)
            throws ValidationException, MappingExecutionException {
        if (!isMappingsValid) {
            mappingPairs = getMappings();
            isMappingsValid = true;
        }
        var mappings = filterConditions(
                filterNull(filterIgnoredFrom(mappingPairs.stream()
                                .filter(m -> eFields.contains(m.toFieldName()))),
                        dObject), dObject, eObject).toList();
        var globalTransformers = context.getGlobalTransformers();
        var fieldTransformers = context.getFieldTransformers();
        var patchSteps = getPatchSteps(mappings, fieldTransformers, globalTransformers);

        before(dObject, eObject);
        processPatchSteps(dObject, eObject, patchSteps);
        processPostMappings(dObject, eObject);
        PatchValidator<E> validator = context.getPatchValidator();
        if (validator != null) {
            validator.validate(eObject);
        }
    }

    private void fetchAnnotationsMetadata() {
        if (context.getAnnotationMetadata() == null) {
            var annotationMetadata = annotationProcessor.process(dClass, eClass);
            for (var entry : annotationMetadata.fieldTransformers().entrySet()) {
                for (var fieldTransformer : entry.getValue()) {
                    addFieldTransformer(entry.getKey(), fieldTransformer);
                }
            }
            context.setAnnotationMetadata(annotationMetadata);
        }

    }

    public E mapWithDefaultCtor(D dObject) {
        try {
            E instance = eClass.getDeclaredConstructor().newInstance();
            patch(dObject, instance);
            return instance;
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new MappingExecutionException("Unable to initialize instance " + eClass.getName(), e);
        }
    }

    public E map(D dObject, Supplier<? extends E> supplier) {
        E instance = supplier.get();
        patch(dObject, instance);
        return instance;
    }

    private void before(D dObject, E eObject) {
        PatchLogger logger = context.getPatchLogger();
        if (logger != null) {
            logger.logObjInfo(dObject, eObject);
        }
    }

    private List<PatchStep<D, E>> getPatchSteps(List<MappingPair> mappings,
                                                Map<String, List<Transformer<?, ?>>> fieldTransformers,
                                                Map<Class<?>, Map<Class<?>, Function<?, ?>>> globalTransformers) {
        List<PatchStep<D, E>> patchSteps = new LinkedList<>();
        for (MappingPair mapping : mappings) {
            // try to find in user's transformers
            PatchApplier<D, E> applier = getSuitableTransformer(mapping,
                    fieldTransformers.getOrDefault(mapping.fromFieldName(), List.of()));
            // fallback to direct mapping
            if (applier == null) {
                applier = getDirectMappingApplier(mapping);
            }
            //fallback to global transformer if no direct mapping is found
            if (applier == null) {
                Function<?, ?> f = globalTransformers
                        .getOrDefault(mapping.fromObjectValueType(), Map.of())
                        .get(mapping.toObjectValueType());
                if (f != null) {
                    applier = wrapToApplier(mapping, f);
                }
            }
            if (applier != null) {
                patchSteps.add(new PatchStep<>(mapping, applier));
            } else if (!mapping.isAutoMapping()) {
                throw new MappingExecutionException("Unable to find suitable transformation for "
                                                    + mapping.fromFieldName() + " (" + mapping.fromObjectValueType()
                                                    + ") to " + mapping.toFieldName()
                                                    + " (" + mapping.toObjectValueType() + ")");
            }
        }
        return patchSteps;
    }

    private void processPatchSteps(D dObject, E eObject, Collection<PatchStep<D, E>> patchSteps) {
        PatchLogger logger = context.getPatchLogger();
        for (PatchStep<D, E> patchStep : patchSteps) {
            try {
                Object newVal = patchStep.applier.apply(dObject, eObject);
                if (logger != null
                    && (context.isGlobalLogChange()
                        || context.getLogChangeFields().getOrDefault(patchStep.mapping.toFieldName(), false)
                        || context.getAnnotationMetadata().logChange().contains(patchStep.mapping.toFieldName()))
                ) {
                    logger.log(patchStep.mapping.fromFieldName(), patchStep.mapping.toFieldName(), newVal);
                }
            } catch (Exception e) {
                throw new MappingExecutionException("Exception during mapping "
                                                    + patchStep.mapping.fromFieldName()
                                                    + " to " + patchStep.mapping.toFieldName(), e);
            }
        }
    }

    private void processPostMappings(D dObject, E eObject) {
        try {
            for (BiConsumer<D, E> consumer : context.getUserPostMappings()) {
                consumer.accept(dObject, eObject);
            }
        } catch (Exception e) {
            throw new MappingExecutionException("Exception while processing postmapping", e);
        }
    }

    private List<MappingPair> getMappings() {
        fetchAnnotationsMetadata();
        List<MappingPair> mappings = new LinkedList<>();
        MappingRegistry mappingRegistry = context.getMappingRegistry();
        try {
            addAnnotationsMappings(mappingRegistry);
            mappingRegistry.scanEntityMappings(dClass, eClass);
        } catch (Exception e) {
            throw new MappingExecutionException("Exception while getting mappings for " + dClass, e);
        }
        for (String fromField : mappingRegistry.getAllResolvedFromObject()) {
            mappings.addAll(mappingRegistry.getFieldMappings(fromField));
        }
        return mappings;
    }

    private void addAnnotationsMappings(MappingRegistry registry) {
        for (Map.Entry<String, String> entry : context.getAnnotationMetadata().mapTo().entrySet()) {
            registry.registerFieldMapping(entry.getKey(), entry.getValue());
        }
    }

    private Stream<MappingPair> filterIgnored(Stream<MappingPair> mappings) {
        return filterIgnoredFrom(filterIgnoredTo(mappings));
    }

    private Stream<MappingPair> filterIgnoredFrom(Stream<MappingPair> mappings) {
        Set<String> ignoredFromFields = context.getIgnoredFromFields();
        Set<String> ignoredFromFieldsAnnotations = context.getAnnotationMetadata().fromIgnore();
        return mappings.filter(m -> !ignoredFromFields.contains(m.fromFieldName())
                                    && !ignoredFromFieldsAnnotations.contains(m.toFieldName()));
    }

    private Stream<MappingPair> filterIgnoredTo(Stream<MappingPair> mappings) {
        Set<String> ignoredToFields = context.getIgnoredToFields();
        Set<String> ignoredToFieldsAnnotations = context.getAnnotationMetadata().fromIgnore();
        return mappings.filter(m -> !ignoredToFields.contains(m.toFieldName())
                                    && !ignoredToFieldsAnnotations.contains(m.fromFieldName()));
    }


    private Stream<MappingPair> filterNull(Stream<MappingPair> mappings, D object) {
        Set<String> ignoredNullFields = context.getIgnoredNullFields();
        Set<String> ignoredNullFieldsAnnotations = context.getAnnotationMetadata().ignoreIfNull();
        return mappings.filter(m ->
                !((context.isGlobalIgnoreNull()
                   || ignoredNullFields.contains(m.fromFieldName())
                   || ignoredNullFieldsAnnotations.contains(m.toFieldName()))
                  && (m.getter().apply(object) == null)));
    }

    private Stream<MappingPair> filterConditions(Stream<MappingPair> mappings, D dObject, E eObject) {
        var conditions = context.getFieldConditions();
        return mappings.filter(m -> conditions.getOrDefault(m.toFieldName(), List.of())
                .stream().allMatch(c -> c.test(dObject, eObject)));
    }

    private PatchApplier<D, E> getSuitableTransformer(MappingPair mapping, Collection<Transformer<?, ?>> transformers) {
        Class<?> fromType = boxed(mapping.fromObjectValueType());
        Class<?> toType = boxed(mapping.toObjectValueType());
        for (Transformer<?, ?> t : transformers) {
            Class<?> inputType = boxed(t.inputType());
            Class<?> outputType = boxed(t.outputType());

            // getter type can be a subclass of inputType
            boolean inputCompatible = inputType.isAssignableFrom(fromType);
            if (!inputCompatible) continue;
            // transformer output can be a subclass of toObjectValueType
            boolean outputCompatible = toType.isAssignableFrom(outputType);
            if (!outputCompatible) continue;

            try {
                return wrapToApplier(mapping, t.function());
            } catch (Exception e) {
                throw new MappingExecutionException("Unable to cast", e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private PatchApplier<D, E> wrapToApplier(MappingPair mapping, Function<?, ?> transformFunction) {
        Function<Object, Object> transformerFunction =
                (Function<Object, Object>) Objects.requireNonNullElse(transformFunction, Function.identity());
        Function<Object, Object> getter = mapping.getter();
        BiConsumer<Object, Object> setter = mapping.setter();
        return new PatchApplier<>(getter, setter, transformerFunction);
    }

    private PatchApplier<D, E> getDirectMappingApplier(MappingPair mapping) {
        Class<?> toClassField = mapping.toObjectValueType();
        Class<?> fromClassField = mapping.fromObjectValueType();
        if (toClassField.isAssignableFrom(fromClassField)) {
            Function<Object, Object> getter = mapping.getter();
            BiConsumer<Object, Object> setter = mapping.setter();
            return new PatchApplier<>(getter, setter, null);
        }
        return null;
    }

    private static Class<?> boxed(Class<?> type) {
        return type.isPrimitive() ? PRIMITIVE_TO_BOXED.get(type) : type;
    }
}
