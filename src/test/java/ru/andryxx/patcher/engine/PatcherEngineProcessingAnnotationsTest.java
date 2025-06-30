package ru.andryxx.patcher.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.andryxx.patcher.annotations.*;
import ru.andryxx.patcher.logging.PatchLogger;
import ru.andryxx.patcher.logging.SystemOutLogger;
import ru.andryxx.patcher.mapping.MappingStrategy;
import ru.andryxx.patcher.mapping.registry.DefaultMappingRegistry;
import ru.andryxx.patcher.mapping.registry.DefaultNamingResolver;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PatcherEngineProcessingAnnotationsTest {
    static <D, E> PatcherEngine<D, E> getEngine(Class<D> dClass, Class<E> eClass, MappingStrategy strategy) {
        PatcherEngine<D, E> engine = new PatcherEngine<>(
                dClass,
                eClass,
                new DefaultMappingRegistry(new DefaultNamingResolver(), strategy)
        );
        engine.setLogger(new SystemOutLogger());
        engine.setGlobalLogChange(true);
        return engine;
    }

    public static class Dto {
        @IgnoreIfNull
        public String name;
        @Ignore
        public int age;
        @Ignore
        boolean active;

        String stringField;

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String getStringField() {
            return stringField;
        }

        public void setStringField(String stringField) {
            this.stringField = stringField;
        }
    }

    public static class Entity {
        public String name;
        public int age;
        @LogChange
        public boolean active;
        @Ignore
        String stringField;

        public LocalDate birthdate;

        public String getStringField() {
            return stringField;
        }

        public void setStringField(String stringField) {
            this.stringField = stringField;
        }
    }

    @Test
    public void shouldProcessIgnore_IgnoreFrom() {
        var engine = getEngine(Dto.class, Entity.class, MappingStrategy.USE_METHODS_AND_FIELDS);

        Dto dto = new Dto();
        dto.name = "test";
        dto.age = 42;
        dto.active = true;

        Entity entity = new Entity();

        engine.patch(dto, entity);

        assertEquals(dto.name, entity.name);
        assertEquals(0, entity.age);
        assertFalse(entity.active);
    }

    @Test
    public void shouldProcessIgnore_IgnoreTo() {
        var engine = getEngine(Dto.class, Entity.class, MappingStrategy.USE_METHODS_AND_FIELDS);

        Dto dto = new Dto();
        dto.name = "foo";
        dto.setStringField("bar");

        Entity entity = new Entity();

        engine.patch(dto, entity);

        assertEquals(dto.name, entity.name);
        assertNull(entity.stringField);
    }

    @Test
    public void shouldProcessIgnore_IgnoreNull() {
        var engine = getEngine(Dto.class, Entity.class, MappingStrategy.USE_METHODS_AND_FIELDS);

        Dto dto = new Dto();

        Entity entity = new Entity();
        entity.name = "foo";

        engine.patch(dto, entity);

        assertNotNull(entity.name);
    }

    @IgnoreIfNull
    public static class DtoGlobalIgnoreNull {
        public String name;
        public int age;
    }

    @Test
    public void shouldProcessIgnore_IgnoreNullGlobal() {
        var engine = getEngine(DtoGlobalIgnoreNull.class, Entity.class, MappingStrategy.USE_METHODS_AND_FIELDS);

        DtoGlobalIgnoreNull dto = new DtoGlobalIgnoreNull();
        Entity entity = new Entity();
        entity.name = "foo";

        engine.patch(dto, entity);
        assertNotNull(entity.name);
    }

    public static class SimpleDto {
        public String name;
        public int age;
        public boolean active;
    }

    @Test
    public void shouldProcessLogChange_LogField() {
        var engine = getEngine(SimpleDto.class, Entity.class, MappingStrategy.USE_METHODS_AND_FIELDS);
        engine.setGlobalLogChange(false);
        PatchLogger mockedLogger = Mockito.mock(PatchLogger.class);
        engine.setLogger(mockedLogger);

        SimpleDto dto = new SimpleDto();
        dto.name = "foo";
        dto.age = 30;
        dto.active = true;

        Entity entity = new Entity();

        engine.patch(dto, entity);
        verify(mockedLogger, times(1))
                .log(eq("active"), eq("active"), eq(true));
    }

    @LogChange
    public static class EntityGlobalLog {
        public String name;
        public int age;
        public boolean active;
    }

    @Test
    public void shouldProcessLogChange_LogGlobal() {
        var engine = getEngine(SimpleDto.class, EntityGlobalLog.class, MappingStrategy.USE_METHODS_AND_FIELDS);
        engine.setGlobalLogChange(false);
        PatchLogger mockedLogger = Mockito.mock(PatchLogger.class);
        engine.setLogger(mockedLogger);

        SimpleDto dto = new SimpleDto();
        dto.name = "foo";
        dto.age = 30;
        dto.active = true;
        EntityGlobalLog entity = new EntityGlobalLog();

        engine.patch(dto, entity);
        verify(mockedLogger, times(1))
                .log(eq("active"), eq("active"), eq(true));
        verify(mockedLogger, times(1))
                .log(eq("name"), eq("name"), eq("foo"));
        verify(mockedLogger, times(1))
                .log(eq("age"), eq("age"), eq(30));
    }

    public static class DtoMap {
        @MapTo("name")
        public String fullName;
    }

    @Test
    public void shouldProcessMapTo() {
        var engine = getEngine(DtoMap.class, Entity.class, MappingStrategy.USE_METHODS_AND_FIELDS);
        DtoMap dto = new DtoMap();
        dto.fullName = "foo";
        Entity entity = new Entity();
        engine.patch(dto, entity);

        assertEquals(dto.fullName, entity.name);
    }

    public static class TransformDto {
        @Transform("strToInt")
        public String age;
        @Transform("strToLocalDate")
        public String birthdate;
    }

    @Test
    public void shouldProcessTransformTo() {
        var engine = getEngine(TransformDto.class, Entity.class, MappingStrategy.USE_METHODS_AND_FIELDS);
        TransformDto dto = new TransformDto();
        dto.age = "30";
        dto.birthdate = "2000-01-01";
        Entity entity = new Entity();

        Transformer.register("strToInt", Transformer.of(String.class, Integer.class, Integer::parseInt));
        Transformer.register("strToLocalDate", Transformer.of(String.class, LocalDate.class, LocalDate::parse));
        engine.patch(dto, entity);

        assertEquals(30, entity.age);
        assertEquals(LocalDate.of(2000, 1, 1), entity.birthdate);
    }
}
