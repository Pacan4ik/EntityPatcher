package ru.andryxx.patcher.engine;

import ru.andryxx.patcher.annotations.*;
import ru.andryxx.patcher.exceptions.MappingExecutionException;

import java.lang.reflect.Field;
import java.util.*;

class AnnotationProcessor {
    record AnnotationMetadata(
            Set<String> fromIgnore,
            Set<String> toIgnore,
            Set<String> ignoreIfNull,
            Set<String> logChange,
            Map<String, String> mapTo,
            Map<String, List<Transformer<?, ?>>> fieldTransformers
    ) {
    }


    public AnnotationMetadata process(Class<?> dClass, Class<?> eClass) {
        Set<String> fromIgnoreList = new HashSet<>();
        Set<String> toIgnoreList = new HashSet<>();
        Set<String> ignoreIfNullList = new HashSet<>();
        Set<String> logChangeList = new HashSet<>();
        Map<String, String> mappingMap = new HashMap<>();
        Map<String, List<Transformer<?, ?>>> transformerMap = new HashMap<>();

        boolean globalIgnoreNull = dClass.isAnnotationPresent(IgnoreIfNull.class);
        boolean globalLogChange = eClass.isAnnotationPresent(LogChange.class);
        // from object fields
        for (Field field : dClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Ignore.class)) {
                fromIgnoreList.add(field.getName());
            }
            if (globalIgnoreNull || field.isAnnotationPresent(IgnoreIfNull.class)) {
                ignoreIfNullList.add(field.getName());
            }
            if (field.isAnnotationPresent(MapTo.class)) {
                MapTo mapTo = field.getAnnotation(MapTo.class);
                mappingMap.put(field.getName(), mapTo.value());
            }
            if (field.isAnnotationPresent(Transform.class)) {
                Transform transform = field.getAnnotation(Transform.class);
                for (String value : transform.value()) {
                    Transformer<?, ?> transformer = Transformer.get(value);
                    if (transformer == null) {
                        throw new MappingExecutionException("Could not find transformer '"
                                                            + value + "' for field '" + field.getName() + "'");
                    }
                    transformerMap.computeIfAbsent(field.getName(), k -> new ArrayList<>()).add(transformer);
                }
            }
        }

        // to objectFields
        for (Field field : eClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Ignore.class)) {
                fromIgnoreList.add(field.getName());
            }
            if (globalLogChange || field.isAnnotationPresent(LogChange.class)) {
                logChangeList.add(field.getName());
            }
        }
        return new AnnotationMetadata(
                fromIgnoreList,
                toIgnoreList,
                ignoreIfNullList,
                logChangeList,
                mappingMap,
                transformerMap
        );
    }
}
