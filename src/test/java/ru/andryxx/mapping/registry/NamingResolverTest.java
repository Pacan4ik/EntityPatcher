package ru.andryxx.mapping.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.andryxx.classes.TestAddress;
import ru.andryxx.classes.TestEntity;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class NamingResolverTest {
    @Test
    public void shouldReturnGetters_SimpleTypes() {
        NamingResolver resolver = new DefaultNamingResolver();

        var opNameGetter = resolver.resolveGetter(TestEntity.class, "name");
        var opAgeGetter = resolver.resolveGetter(TestEntity.class, "age");

        Assertions.assertTrue(opNameGetter.isPresent());
        Assertions.assertTrue(opAgeGetter.isPresent());
        Assertions.assertEquals("getName", opNameGetter.get().getName());
        Assertions.assertEquals("getAge", opAgeGetter.get().getName());
    }

    @Test
    public void shouldReturnGetter_AddressType() {
        ru.andryxx.mapping.registry.NamingResolver resolver = new DefaultNamingResolver();

        var opAddressGetter = resolver.resolveGetter(TestEntity.class, "address");

        Assertions.assertTrue(opAddressGetter.isPresent());
        Assertions.assertEquals("getAddress", opAddressGetter.get().getName());
    }

    @Test
    public void shouldReturnGetter_BooleanType() {
        ru.andryxx.mapping.registry.NamingResolver resolver = new DefaultNamingResolver();

        var opActiveGetter = resolver.resolveGetter(TestEntity.class, "active");

        Assertions.assertTrue(opActiveGetter.isPresent());
        Assertions.assertEquals("isActive", opActiveGetter.get().getName());
    }

    @Test
    public void shouldReturnEmptyWhenGetter_MethodWithParams() {
        ru.andryxx.mapping.registry.NamingResolver resolver = new DefaultNamingResolver();

        var opMethod = resolver.resolveGetter(TestEntity.class, "something");

        try {
            TestEntity.class.getMethod("getSomething", int.class);
        } catch (NoSuchMethodException e) {
            assumeTrue(false, "Method getSomething(int) does not exist");
        }
        Assertions.assertFalse(opMethod.isPresent());
    }

    @Test
    public void shouldReturnSetters() {
        ru.andryxx.mapping.registry.NamingResolver resolver = new DefaultNamingResolver();

        var opNameSetter = resolver.resolveSetter(TestEntity.class, "name");
        var opAgeSetter = resolver.resolveSetter(TestEntity.class, "age");
        var opAddressSetter = resolver.resolveSetter(TestEntity.class, "address");

        assumeTrue(opNameSetter.isPresent());
        assumeTrue(opAgeSetter.isPresent());
        assumeTrue(opAddressSetter.isPresent());

        Assertions.assertEquals(String.class, opNameSetter.get().getParameters()[0].getType());
        Assertions.assertEquals(int.class, opAgeSetter.get().getParameters()[0].getType());
        Assertions.assertEquals(TestAddress.class, opAddressSetter.get().getParameters()[0].getType());
    }

    @Test
    public void shouldReturnEmpty_PrivateSetter() {
        ru.andryxx.mapping.registry.NamingResolver resolver = new DefaultNamingResolver();

        var opMethod = resolver.resolveSetter(TestEntity.class, "someField");

        Assertions.assertFalse(opMethod.isPresent());
    }

    @Test
    public void shouldReturnPublicField() {
        ru.andryxx.mapping.registry.NamingResolver resolver = new DefaultNamingResolver();

        var opField = resolver.resolveField(TestEntity.class, "publicField");

        Assertions.assertTrue(opField.isPresent());
    }

    @Test
    public void shouldReturnEmpty_PrivateField() {
        ru.andryxx.mapping.registry.NamingResolver resolver = new DefaultNamingResolver();

        var opField = resolver.resolveField(TestEntity.class, "someField");

        Assertions.assertFalse(opField.isPresent());
    }

    @Test
    public void shouldReturnEmpty_StaticGetter() {
        ru.andryxx.mapping.registry.NamingResolver resolver = new DefaultNamingResolver();

        var opMethod = resolver.resolveGetter(TestEntity.class, "staticField");

        Assertions.assertFalse(opMethod.isPresent());
    }

    @Test
    public void shouldReturnEmpty_StaticSetter() {
        ru.andryxx.mapping.registry.NamingResolver resolver = new DefaultNamingResolver();

        var opMethod = resolver.resolveSetter(TestEntity.class, "staticField");

        Assertions.assertFalse(opMethod.isPresent());
    }

    @Test
    public void shouldReturnEmpty_StaticPublicField() {
        ru.andryxx.mapping.registry.NamingResolver resolver = new DefaultNamingResolver();

        var opMethod = resolver.resolveField(TestEntity.class, "staticField");

        Assertions.assertFalse(opMethod.isPresent());
    }
}
