package ru.andryxx.mapping.registry;

import org.junit.jupiter.api.Test;
import ru.andryxx.classes.TestDTO;
import ru.andryxx.classes.TestEntity;
import ru.andryxx.exceptions.MatchingPathException;
import ru.andryxx.mapping.MappingStrategy;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

public class MappingRegistryTest {
    private static final NamingResolver resolver = new DefaultNamingResolver();

    @Test
    public void shouldResolveMethods_OnlyMethodsStrategy() {
        MappingRegistry registry = new DefaultMappingRegistry(resolver, MappingStrategy.USE_METHODS);

        registry.scanEntityMappings(TestDTO.class, TestEntity.class);

        var dtoFields = registry.getAllFromObjectFields();
        assertThat(dtoFields, equalTo(Set.of("age", "active")));

        var opMapPairAge = registry.getFieldMapping("age");
        assertTrue(opMapPairAge.isPresent());
        assertEquals(int.class, opMapPairAge.get().fromObjectValueType());
        assertEquals(int.class, opMapPairAge.get().toObjectValueType());
        assertEquals("getAge", opMapPairAge.get().fromName());
        assertEquals("setAge", opMapPairAge.get().toName());

        var opMapPairActive = registry.getFieldMapping("active");
        assertTrue(opMapPairActive.isPresent());
        assertEquals(boolean.class, opMapPairActive.get().fromObjectValueType());
        assertEquals(boolean.class, opMapPairActive.get().toObjectValueType());
        assertEquals("isActive", opMapPairActive.get().fromName());
        assertEquals("setActive", opMapPairActive.get().toName());
    }

    @Test
    public void shouldResolveFields_OnlyFieldsStrategy() {
        MappingRegistry registry = new DefaultMappingRegistry(resolver, MappingStrategy.USE_FIELDS);

        registry.scanEntityMappings(TestDTO.class, TestEntity.class);

        var dtoFields = registry.getAllFromObjectFields();
        assertThat(dtoFields, equalTo(Set.of("publicField")));

        var opMapPair = registry.getFieldMapping("publicField");
        assertTrue(opMapPair.isPresent());
        assertEquals(String.class, opMapPair.get().fromObjectValueType());
        assertEquals(String.class, opMapPair.get().toObjectValueType());
    }

    @Test
    public void shouldResolveBoth_MethodsAndFieldsStrategy() {
        MappingRegistry registry = new DefaultMappingRegistry(resolver, MappingStrategy.USE_METHODS_AND_FIELDS);

        registry.scanEntityMappings(TestDTO.class, TestEntity.class);
        var dtoFields = registry.getAllFromObjectFields();
        assertThat(dtoFields, equalTo(Set.of("age", "active", "publicField")));
    }


    @Test
    public void shouldProcessExplicitMappings_CorrectMappings() {
        MappingRegistry registry = new DefaultMappingRegistry(resolver, MappingStrategy.USE_METHODS_AND_FIELDS);

        registry.registerFieldMapping("fullName", "name");
        assertDoesNotThrow(() -> registry.scanEntityMappings(TestDTO.class, TestEntity.class));

        var dtoFields = registry.getAllFromObjectFields();
        assertTrue(dtoFields.contains("fullName"));
        var opMapPair = registry.getFieldMapping("fullName");
        assertTrue(opMapPair.isPresent());
        assertEquals("getFullName", opMapPair.get().fromName());
        assertEquals("setName", opMapPair.get().toName());
    }

    @Test
    public void shouldThrowException_IncorrectExplicitMappingsOnFrom() {
        MappingRegistry registry = new DefaultMappingRegistry(resolver, MappingStrategy.USE_METHODS_AND_FIELDS);

        registry.registerFieldMapping("name", "name");

        assertThrows(MatchingPathException.class, () -> registry.scanEntityMappings(TestDTO.class, TestEntity.class));
    }

    @Test
    public void shouldThrowException_IncorrectExplicitMappingsOnTo() {
        MappingRegistry registry = new DefaultMappingRegistry(resolver, MappingStrategy.USE_METHODS_AND_FIELDS);

        registry.registerFieldMapping("fullName", "fullName");

        assertThrows(MatchingPathException.class, () -> registry.scanEntityMappings(TestDTO.class, TestEntity.class));
    }

    @Test
    public void shouldReturnCorrectGettersAndSetters_Methods() {
        MappingRegistry registry = new DefaultMappingRegistry(resolver, MappingStrategy.USE_METHODS);

        registry.scanEntityMappings(TestDTO.class, TestEntity.class);
        TestDTO dto = new TestDTO();
        dto.setAge(20);
        TestEntity entity = new TestEntity();

        var opMapping = registry.getFieldMapping("age");
        assertTrue(opMapping.isPresent());
        var dtoAge = opMapping.get().getter().apply(dto);
        assertEquals(Integer.class, dtoAge.getClass());
        assertEquals(20, (Integer) dtoAge);

        opMapping.get().setter().accept(entity, 20);
        assertEquals(20, entity.getAge());
    }
}
