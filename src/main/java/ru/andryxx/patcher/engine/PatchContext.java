package ru.andryxx.patcher.engine;

import ru.andryxx.patcher.logging.PatchLogger;
import ru.andryxx.patcher.mapping.registry.MappingRegistry;
import ru.andryxx.patcher.validation.PatchValidator;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

class PatchContext<D, E> {
    private final Map<Class<?>, Map<Class<?>, Function<?, ?>>> globalTransformers = new ConcurrentHashMap<>();
    private final Map<String, List<Transformer<?, ?>>> fieldTransformers = new ConcurrentHashMap<>();

    private final Map<String, List<BiPredicate<D, E>>> fieldConditions = new ConcurrentHashMap<>();

    private boolean globalIgnoreNull = false;
    private final HashSet<String> ignoredNullFields = new HashSet<>();
    private final HashSet<String> ignoredFromFields = new HashSet<>();
    private final HashSet<String> ignoredToFields = new HashSet<>();

    private boolean globalLogChange = false;
    private final Map<String, Boolean> logChangeFields = new ConcurrentHashMap<>();

    private final LinkedList<BiConsumer<D, E>> userPostMappings = new LinkedList<>();

    private PatchLogger patchLogger;
    private PatchValidator<E> patchValidator;
    private MappingRegistry mappingRegistry;


    public PatchContext() {

    }

    public Map<Class<?>, Map<Class<?>, Function<?, ?>>> getGlobalTransformers() {
        return globalTransformers;
    }

    public Map<String, List<Transformer<?, ?>>> getFieldTransformers() {
        return fieldTransformers;
    }

    public Map<String, List<BiPredicate<D, E>>> getFieldConditions() {
        return fieldConditions;
    }

    public boolean isGlobalIgnoreNull() {
        return globalIgnoreNull;
    }

    public void setGlobalIgnoreNull(boolean globalIgnoreNull) {
        this.globalIgnoreNull = globalIgnoreNull;
    }

    public HashSet<String> getIgnoredNullFields() {
        return ignoredNullFields;
    }

    public HashSet<String> getIgnoredFromFields() {
        return ignoredFromFields;
    }

    public HashSet<String> getIgnoredToFields() {
        return ignoredToFields;
    }

    public LinkedList<BiConsumer<D, E>> getUserPostMappings() {
        return userPostMappings;
    }

    public PatchLogger getPatchLogger() {
        return patchLogger;
    }

    public void setPatchLogger(PatchLogger patchLogger) {
        this.patchLogger = patchLogger;
    }

    public PatchValidator<E> getPatchValidator() {
        return patchValidator;
    }

    public void setPatchValidator(PatchValidator<E> patchValidator) {
        this.patchValidator = patchValidator;
    }

    public MappingRegistry getMappingRegistry() {
        return mappingRegistry;
    }

    public void setMappingRegistry(MappingRegistry mappingRegistry) {
        this.mappingRegistry = mappingRegistry;
    }

    public boolean isGlobalLogChange() {
        return globalLogChange;
    }

    public void setGlobalLogChange(boolean globalLogChange) {
        this.globalLogChange = globalLogChange;
    }

    public Map<String, Boolean> getLogChangeFields() {
        return logChangeFields;
    }
}
