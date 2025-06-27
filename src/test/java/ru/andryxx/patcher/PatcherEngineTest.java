package ru.andryxx.patcher;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.andryxx.classes.TestAddress;
import ru.andryxx.classes.TestDTO;
import ru.andryxx.classes.TestEntity;
import ru.andryxx.exceptions.MappingExecutionException;
import ru.andryxx.exceptions.ValidationException;
import ru.andryxx.logging.PatchLogger;
import ru.andryxx.logging.SystemOutLogger;
import ru.andryxx.mapping.MappingStrategy;
import ru.andryxx.mapping.registry.DefaultMappingRegistry;
import ru.andryxx.mapping.registry.DefaultNamingResolver;
import ru.andryxx.validation.PatchValidator;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PatcherEngineTest {
    private static PatcherEngine<TestDTO, TestEntity> getEngine(MappingStrategy strategy) {
        var p = new PatcherEngine<>(
                TestDTO.class,
                TestEntity.class,
                new DefaultMappingRegistry(new DefaultNamingResolver(), strategy)
        );
        p.setLogger(new SystemOutLogger());
        p.setGlobalLogChange(true);
        return p;
    }

    @Test
    public void shouldPatch_OnlyMethodsStrategy() {
        var engine = getEngine(MappingStrategy.USE_METHODS);

        TestDTO dto = new TestDTO();
        dto.setAge(10);
        dto.setActive(true);
        dto.setPublicField("foo");

        TestEntity entity = new TestEntity();
        entity.setAge(20);
        entity.setActive(false);
        entity.publicField = "bar";

        engine.patch(dto, entity);

        assertEquals(10, entity.getAge());
        assertTrue(entity.isActive());
        assertEquals("bar", entity.publicField);
    }

    @Test
    public void shouldPatch_OnlyFieldsStrategy() {
        var engine = getEngine(MappingStrategy.USE_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setAge(10);
        dto.publicField = "foo";

        TestEntity entity = new TestEntity();
        entity.setAge(20);
        entity.publicField = "bar";

        engine.patch(dto, entity);
        assertEquals("foo", entity.publicField);
        assertEquals(20, entity.getAge());
    }

    @Test
    public void shouldPatch_MethodsAndFieldsStrategy() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setAge(10);
        dto.publicField = "foo";
        dto.setActive(true);

        TestEntity entity = new TestEntity();
        entity.setAge(20);
        entity.publicField = "bar";
        entity.setActive(false);

        engine.patch(dto, entity);

        assertEquals(10, entity.getAge());
        assertEquals("foo", entity.publicField);
        assertTrue(entity.isActive());
    }

    @Test
    public void shouldPatchWhenExplicitlyDefined_PresentedOnce() {
        var engine = getEngine(MappingStrategy.USE_METHODS);

        TestDTO dto = new TestDTO();
        dto.setFullName("foo");
        TestEntity entity = new TestEntity();
        entity.setName("bar");
        engine.addStaticFieldMapping("fullName", "name");

        engine.patch(dto, entity);

        assertEquals("foo", entity.getName());
    }

    @Test
    public void shouldThrowExceptionWhenExplicitlyDefined_NotPresentedInEntity() {
        var engine = getEngine(MappingStrategy.USE_METHODS);

        TestDTO dto = new TestDTO();
        dto.setFullName("foo");
        TestEntity entity = new TestEntity();
        entity.setName("bar");
        engine.addStaticFieldMapping("fullName", "notfound");

        assertThrows(MappingExecutionException.class, () -> engine.patch(dto, entity));
    }

    @Test
    public void shouldThrowExceptionWhenExplicitlyDefined_NotPresentedInDto() {
        var engine = getEngine(MappingStrategy.USE_METHODS);

        TestDTO dto = new TestDTO();
        dto.setFullName("foo");
        TestEntity entity = new TestEntity();
        entity.setName("bar");
        engine.addStaticFieldMapping("notfound", "name");

        assertThrows(MappingExecutionException.class, () -> engine.patch(dto, entity));
    }

    @Test
    public void shouldPatchExplicitlyDefinedInsteadOfAuto() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setFullName("foo");
        dto.publicField = "bar";

        TestEntity entity = new TestEntity();
        entity.setName("bob");
        entity.publicField = "field";

        engine.addStaticFieldMapping("fullName", "publicField");
        engine.addStaticFieldMapping("publicField", "name");

        engine.patch(dto, entity);

        assertEquals("bar", entity.getName());
        assertEquals("foo", entity.publicField);
    }

    @Test
    public void shouldPatchExplicitlyDefinedInsteadOfAuto_MultipleDestinations() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setFullName("foo");
        dto.publicField = "bar";

        TestEntity entity = new TestEntity();
        entity.setName("bob");
        entity.publicField = "field";

        engine.addStaticFieldMapping("fullName", "publicField");
        engine.addStaticFieldMapping("fullName", "name");

        engine.patch(dto, entity);

        assertEquals("foo", entity.getName());
        assertEquals("foo", entity.publicField);
    }

    @Test
    public void shouldPatchExplicitlyDefinedInsteadOfAuto_UseLastDefinedTo() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setFullName("foo");
        dto.publicField = "bar";

        TestEntity entity = new TestEntity();
        entity.setName("bob");
        entity.publicField = "field";

        engine.addStaticFieldMapping("publicField", "name");
        engine.addStaticFieldMapping("fullName", "name");

        engine.patch(dto, entity);

        assertEquals("foo", entity.getName());
        assertEquals("bar", entity.publicField);
    }

    @Test
    public void shouldUseGlobalConverters_StringToLocalDate() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setBirthdate("2000-01-01");

        TestEntity entity = new TestEntity();

        engine.addGlobalTransformer(Transformer.of(String.class, LocalDate.class, LocalDate::parse));

        engine.patch(dto, entity);

        assertEquals(LocalDate.of(2000, 1, 1), entity.getBirthdate());
    }

    @Test
    public void shouldThrowExceptionWhenGlobalConverterFails_StringToLocalDate() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setBirthdate("2000-янв-01");

        TestEntity entity = new TestEntity();

        engine.addGlobalTransformer(Transformer.of(String.class, LocalDate.class, LocalDate::parse));

        assertThrows(MappingExecutionException.class, () -> engine.patch(dto, entity));
    }

    @Test
    public void shouldUseFieldConverters_StringToLocalDate() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setBirthdate("2000-01-01");

        TestEntity entity = new TestEntity();

        engine.addFieldTransformer("birthdate", Transformer.of(String.class, LocalDate.class, LocalDate::parse));

        engine.patch(dto, entity);

        assertEquals(LocalDate.of(2000, 1, 1), entity.getBirthdate());
    }

    @Test
    public void shouldThrowExceptionsWhenFieldConverterFails_StringToLocalDate() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setBirthdate("2000-янв-01");

        TestEntity entity = new TestEntity();

        engine.addFieldTransformer("birthdate", Transformer.of(String.class, LocalDate.class, LocalDate::parse));

        assertThrows(MappingExecutionException.class, () -> engine.patch(dto, entity));
    }

    @Test
    public void shouldMapSubClasses_StringToObjectUpcast() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setBirthdate("2000-01-01");

        TestEntity entity = new TestEntity();

        engine.addStaticFieldMapping("birthdate", "publicObjectField");

        assertDoesNotThrow(() -> engine.patch(dto, entity));
        assertEquals("2000-01-01", entity.publicObjectField);
    }

    @Test
    public void shouldTransformWithCasting_LocalDateAfterTransformToObjectUpcast() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setBirthdate("2000-01-01");

        TestEntity entity = new TestEntity();

        engine.addStaticFieldMapping("birthdate", "publicObjectField");
        engine.addFieldTransformer("birthdate", Transformer.of(String.class, LocalDate.class, LocalDate::parse));

        assertDoesNotThrow(() -> engine.patch(dto, entity));
        assertEquals(LocalDate.of(2000, 1, 1), entity.publicObjectField);
    }

    public static int ObjectToIntStub(Object object) {
        try {
            return Integer.parseInt(object.toString());
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    @Test
    public void shouldTransformWithCasting_StringBeforeTransformToObjectUpcast() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.publicField = "30";
        TestEntity entity = new TestEntity();

        engine.addStaticFieldMapping("publicField", "age");
        engine.addFieldTransformer("publicField", Transformer.of(Object.class, int.class,
                PatcherEngineTest::ObjectToIntStub));

        engine.patch(dto, entity);
        assertEquals(30, entity.getAge());
    }

    @Test
    public void shouldPatchWithCondition_TrueCondition() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setAge(30);
        TestEntity entity = new TestEntity();

        engine.addFieldCondition("age", (d, e) -> d.getAge() > 20);

        engine.patch(dto, entity);
        assertEquals(30, entity.getAge());
    }

    @Test
    public void shouldPatchWithCondition_FalseCondition() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setAge(10);
        TestEntity entity = new TestEntity();
        entity.setAge(20);

        engine.addFieldCondition("age", (d, e) -> d.getAge() > 20);

        engine.patch(dto, entity);
        assertEquals(20, entity.getAge());
    }

    @Test
    public void shouldIgnoreNull_Global() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setAge(10);
        dto.setFullName(null);
        dto.setActive(true);

        TestEntity entity = new TestEntity();
        entity.setAge(20);
        entity.setName("foo");
        entity.setActive(false);
        entity.publicField = "bar";

        engine.addStaticFieldMapping("fullName", "name");
        engine.setGlobalIgnoreNull(true);
        engine.patch(dto, entity);

        assertEquals("foo", entity.getName());
        assertEquals(10, entity.getAge());
        assertEquals("bar", entity.publicField);
        assertTrue(entity.isActive());
    }

    @Test
    public void shouldIgnoreNull_Field() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setFullName(null);
        dto.publicField = null;

        TestEntity entity = new TestEntity();
        entity.setName("foo");
        entity.publicField = "bar";

        engine.addStaticFieldMapping("fullName", "name");
        engine.ignoreNullField("fullName", true);

        engine.patch(dto, entity);

        assertEquals("foo", entity.getName());
        assertNull(entity.publicField);
    }

    @Test
    public void shouldIgnoreField_IgnoreFrom() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.publicField = "foo";
        dto.setFullName("bar");
        dto.setAge(10);

        TestEntity entity = new TestEntity();
        entity.setName("foobar");
        entity.publicField = "barfoo";

        engine.addStaticFieldMapping("fullName", "name");
        engine.ignoreFromField("fullName", true);
        engine.ignoreFromField("publicField", true);

        engine.patch(dto, entity);

        assertEquals("foobar", entity.getName());
        assertEquals(10, entity.getAge());
        assertEquals("barfoo", entity.publicField);
    }

    @Test
    public void shouldIgnoreField_IgnoreTo() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setAge(10);
        dto.setFullName("bar");
        dto.setActive(true);

        TestEntity entity = new TestEntity();
        entity.setName("foobar");

        engine.addStaticFieldMapping("fullName", "name");
        engine.ignoreToField("name", true);
        engine.ignoreToField("active", true);

        engine.patch(dto, entity);

        assertEquals("foobar", entity.getName());
        assertEquals(10, entity.getAge());
        assertFalse(entity.isActive());
    }

    @Test
    public void shouldProcessPostMappings() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setAge(10);
        dto.setFullName("foo");
        dto.setActive(true);
        dto.setBirthdate("2000-01-01");
        dto.publicField = "city 123";

        TestEntity entity = new TestEntity();
        entity.setName("foobar");
        entity.publicField = "barfoo";

        engine.addStaticFieldMapping("fullName", "name");
        engine.addGlobalTransformer(Transformer.of(String.class, LocalDate.class, LocalDate::parse));

        engine.addPostMapping((d, e) -> {
            String[] a = d.publicField.split(" ");
            e.setAddress(new TestAddress(a[0], Integer.parseInt(a[1])));
        });

        engine.patch(dto, entity);

        assertEquals("city", entity.getAddress().getCity());
        assertEquals(123, entity.getAddress().getZipCode());
    }

    @Test
    public void shouldUseLogger_Field() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);
        PatchLogger mockedLogger = Mockito.mock(PatchLogger.class);
        ArgumentCaptor<String> fromFieldCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> toFieldCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> newValueCaptor = ArgumentCaptor.forClass(Object.class);
        engine.setLogger(mockedLogger);

        TestDTO dto = new TestDTO();
        dto.setAge(10);
        dto.publicField = "foo";
        TestEntity entity = new TestEntity();
        entity.setAge(20);
        engine.setGlobalLogChange(false);
        engine.setFieldLogChange("age", true);
        engine.setFieldLogChange("publicField", true);

        engine.patch(dto, entity);

        verify(mockedLogger, times(2))
                .log(fromFieldCaptor.capture(), toFieldCaptor.capture(), newValueCaptor.capture());

        assertThat(fromFieldCaptor.getAllValues(), containsInAnyOrder("age", "publicField"));
        assertThat(toFieldCaptor.getAllValues(), containsInAnyOrder("age", "publicField"));
        assertThat(newValueCaptor.getAllValues(), containsInAnyOrder("foo", 10));
    }

    @Test
    public void shouldUseLogger_Global() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);
        PatchLogger mockedLogger = Mockito.mock(PatchLogger.class);
        ArgumentCaptor<String> fromFieldCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> toFieldCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> newValueCaptor = ArgumentCaptor.forClass(Object.class);
        engine.setLogger(mockedLogger);

        TestDTO dto = new TestDTO();
        dto.setAge(10);
        dto.publicField = "foo";
        dto.setFullName("bar");
        dto.setBirthdate("2000-01-01");

        TestEntity entity = new TestEntity();
        entity.setAge(20);
        engine.setGlobalLogChange(true);
        engine.addStaticFieldMapping("fullName", "name");

        engine.addGlobalTransformer(Transformer.of(String.class, LocalDate.class, LocalDate::parse));

        engine.patch(dto, entity);

        verify(mockedLogger, times(5))
                .log(fromFieldCaptor.capture(), toFieldCaptor.capture(), newValueCaptor.capture());

        assertThat(fromFieldCaptor.getAllValues(),
                containsInAnyOrder("age", "publicField", "fullName", "birthdate", "active")
        );
        assertThat(toFieldCaptor.getAllValues(),
                containsInAnyOrder("age", "publicField", "name", "birthdate", "active")
        );
    }

    @Test
    public void shouldUseValidator_Pass() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);
        PatchValidator<TestEntity> mockedValidator = Mockito.mock(PatchValidator.class);

        TestDTO dto = new TestDTO();
        dto.setAge(10);
        dto.publicField = "foo";
        dto.setFullName("bar");
        dto.setBirthdate("2000-01-01");

        TestEntity entity = new TestEntity();
        entity.setAge(20);
        engine.setGlobalLogChange(true);
        engine.addStaticFieldMapping("fullName", "name");

        engine.setValidator(mockedValidator);

        engine.patch(dto, entity);

        verify(mockedValidator, times(1));
    }

    @Test
    public void shouldUseValidator_Fail() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);
        PatchValidator<TestEntity> mockedValidator = Mockito.mock(PatchValidator.class);
        doThrow(new ValidationException(""))
                .when(mockedValidator).validate(any(TestEntity.class));
        engine.setValidator(mockedValidator);

        TestDTO dto = new TestDTO();
        dto.setAge(10);
        dto.publicField = "foo";
        dto.setFullName("bar");
        dto.setBirthdate("2000-01-01");

        TestEntity entity = new TestEntity();
        entity.setAge(20);
        engine.setGlobalLogChange(true);
        engine.addStaticFieldMapping("fullName", "name");

        engine.setValidator(mockedValidator);

        assertThrows(ValidationException.class, () -> engine.patch(dto, entity));
    }

    @Test
    public void shouldChangeConditions() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setAge(10);
        dto.publicField = "foo";
        dto.setFullName("bar");
        dto.setBirthdate("2000-01-01");

        TestEntity entity = new TestEntity();
        TestEntity entity2 = new TestEntity();
        engine.addStaticFieldMapping("fullName", "name");
        engine.addGlobalTransformer(Transformer.of(String.class, LocalDate.class, LocalDate::parse));

        engine.patch(dto, entity);
        assertEquals(10, entity.getAge());
        assertEquals("foo", entity.publicField);
        assertEquals("bar", entity.getName());
        assertEquals(LocalDate.of(2000, 1, 1), entity.getBirthdate());

        engine.ignoreToField("birthdate", true);
        engine.ignoreNullField("publicField", true);

        dto.publicField = null;
        entity2.publicField = "ignored";

        engine.patch(dto, entity2);
        assertEquals(10, entity2.getAge());
        assertEquals("ignored", entity2.publicField);
        assertNull(entity2.getBirthdate());
        assertEquals("bar", entity2.getName());
    }

    @Test
    public void shouldReloadMappings() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setFullName("name");
        dto.setAge(10);

        TestEntity entity = new TestEntity();
        TestEntity entity2 = new TestEntity();

        engine.addStaticFieldMapping("fullName", "name");

        engine.patch(dto, entity);
        assertEquals(10, entity.getAge());
        assertEquals("name", entity.getName());

        System.out.println("---");
        engine.addStaticFieldMapping("fullName", "publicField");
        engine.addStaticFieldMapping("age", "publicObjectField");
        engine.addGlobalTransformer(Transformer.of(int.class, Object.class, Integer::valueOf));

        engine.patch(dto, entity2);
        assertEquals("name", entity2.publicField);
        assertEquals("name", entity2.getName());
        assertEquals(10, entity2.publicObjectField);
    }

    @Test
    public void shouldPatchOnlyFields() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);

        TestDTO dto = new TestDTO();
        dto.setFullName("name");
        dto.setAge(10);
        dto.setActive(true);

        TestEntity entity = new TestEntity();

        engine.addStaticFieldMapping("fullName", "name");
        engine.patchSelective(Set.of("name", "age"), dto, entity);

        assertEquals(10, entity.getAge());
        assertEquals("name", entity.getName());
        assertFalse(entity.isActive());
    }

    @Test
    public void shouldMapWithDefaultCtor_SameAsPatchOnNew() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);
        TestDTO dto = new TestDTO();
        dto.setFullName("name");
        dto.setAge(10);
        dto.setActive(true);
        engine.addStaticFieldMapping("fullName", "name");

        TestEntity entity = new TestEntity();
        TestEntity mapEntity = engine.mapWithDefaultCtor(dto);
        engine.patch(dto, entity);

        assertEquals(entity, mapEntity);
    }

    @Test
    public void shouldMapWithSupplier() {
        var engine = getEngine(MappingStrategy.USE_METHODS_AND_FIELDS);
        TestDTO dto = new TestDTO();
        dto.setFullName("name");
        dto.setAge(10);
        dto.setActive(true);
        engine.addStaticFieldMapping("fullName", "name");
        engine.ignoreToField("publicField", true);

        Supplier<TestEntity> supplier = () -> {
            TestEntity entity = new TestEntity();
            entity.publicField = "publicField";
            return entity;
        };

        TestEntity entity = engine.map(dto, supplier);

        assertEquals("name", entity.getName());
        assertEquals(10, entity.getAge());
        assertTrue(entity.isActive());
        assertEquals("publicField", entity.publicField);
    }
}
