package ru.andryxx.mapping.registry;

import org.junit.jupiter.api.Test;
import ru.andryxx.classes.TestDTO;
import ru.andryxx.classes.TestEntity;
import ru.andryxx.exceptions.MatchingPathException;
import ru.andryxx.mapping.MappingPair;
import ru.andryxx.mapping.MappingStrategy;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

public class MappingRegistryTest {
    private static final NamingResolver resolver = new DefaultNamingResolver();

    @Test
    public void shouldResolveMethods_OnlyMethodsStrategy() {
        MappingRegistry registry = new DefaultMappingRegistry(resolver, MappingStrategy.USE_METHODS);

        registry.scanEntityMappings(TestDTO.class, TestEntity.class);

        var dtoFields = registry.getAllResolvedFromObject();
        assertThat(dtoFields, containsInAnyOrder("active", "age", "birthdate"));

        var ageMappings = registry.getFieldMappings("age");
        assertFalse(ageMappings.isEmpty());
        var ageMapping = ageMappings.iterator().next();
        assertEquals(int.class, ageMapping.fromObjectValueType());
        assertEquals(int.class, ageMapping.toObjectValueType());
        assertEquals("getAge", ageMapping.fromName());
        assertEquals("setAge", ageMapping.toName());

        var activeMappings = registry.getFieldMappings("active");
        assertFalse(activeMappings.isEmpty());
        var activeMapping = activeMappings.iterator().next();
        assertEquals(boolean.class, activeMapping.fromObjectValueType());
        assertEquals(boolean.class, activeMapping.toObjectValueType());
        assertEquals("isActive", activeMapping.fromName());
        assertEquals("setActive", activeMapping.toName());
    }

    @Test
    public void shouldResolveFields_OnlyFieldsStrategy() {
        MappingRegistry registry = new DefaultMappingRegistry(resolver, MappingStrategy.USE_FIELDS);

        registry.scanEntityMappings(TestDTO.class, TestEntity.class);

        var dtoFields = registry.getAllResolvedFromObject();
        assertThat(dtoFields, equalTo(Set.of("publicField")));

        var mappings = registry.getFieldMappings("publicField");
        assertFalse(mappings.isEmpty());
        var mapping = mappings.iterator().next();
        assertEquals(String.class, mapping.fromObjectValueType());
        assertEquals(String.class, mapping.toObjectValueType());
    }

    @Test
    public void shouldResolveBoth_MethodsAndFieldsStrategy() {
        MappingRegistry registry = new DefaultMappingRegistry(resolver, MappingStrategy.USE_METHODS_AND_FIELDS);

        registry.scanEntityMappings(TestDTO.class, TestEntity.class);
        var dtoFields = registry.getAllResolvedFromObject();
        assertThat(dtoFields, containsInAnyOrder("age", "active", "publicField", "birthdate"));
    }


    @Test
    public void shouldProcessExplicitMappings_CorrectMappings() {
        MappingRegistry registry = new DefaultMappingRegistry(resolver, MappingStrategy.USE_METHODS_AND_FIELDS);

        registry.registerFieldMapping("fullName", "name");
        assertDoesNotThrow(() -> registry.scanEntityMappings(TestDTO.class, TestEntity.class));

        var dtoFields = registry.getAllResolvedFromObject();
        System.out.println(dtoFields);
        assertTrue(dtoFields.contains("fullName"));
        var mappings = registry.getFieldMappings("fullName");
        assertFalse(mappings.isEmpty());
        var mapping = mappings.iterator().next();
        assertEquals("getFullName", mapping.fromName());
        assertEquals("setName", mapping.toName());
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

        var mappings = registry.getFieldMappings("age");
        assertFalse(mappings.isEmpty());
        MappingPair mapping = mappings.iterator().next();
        var dtoAge = mapping.getter().apply(dto);
        assertEquals(Integer.class, dtoAge.getClass());
        assertEquals(20, (Integer) dtoAge);

        mapping.setter().accept(entity, 20);
        assertEquals(20, entity.getAge());
    }
}
