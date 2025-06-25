package ru.andryxx;

import ru.andryxx.mapping.MappingStrategy;

import java.time.LocalDate;
import java.time.LocalDateTime;

class PatcherFactory {
    private PatcherFactory() {
    }

    public static <D, E> Patcher<D, E> createDefault(Class<D> dClass, Class<E> eClass) {
        return Patcher.emptyPatcher(dClass, eClass)
                .withMappingStrategy(MappingStrategy.USE_METHODS_AND_FIELDS)
                .withTransformer(Transformer.of(String.class, LocalDateTime.class, LocalDateTime::parse))
                .withTransformer(Transformer.of(String.class, LocalDate.class, LocalDate::parse))
                .withTransformer(Transformer.of(int.class, Integer.class, Integer::valueOf))
                .withTransformer(Transformer.of(long.class, Long.class, Long::valueOf))
                .withTransformer(Transformer.of(int.class, Long.class, Long::valueOf));
    }
}
