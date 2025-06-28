package ru.andryxx.patcher.mapping.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.andryxx.patcher.classes.TestAddress;
import ru.andryxx.patcher.classes.TestEntity;

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
        NamingResolver resolver = new DefaultNamingResolver();

        var opAddressGetter = resolver.resolveGetter(TestEntity.class, "address");

        Assertions.assertTrue(opAddressGetter.isPresent());
        Assertions.assertEquals("getAddress", opAddressGetter.get().getName());
    }

    @Test
    public void shouldReturnGetter_BooleanType() {
        NamingResolver resolver = new DefaultNamingResolver();

        var opActiveGetter = resolver.resolveGetter(TestEntity.class, "active");

        Assertions.assertTrue(opActiveGetter.isPresent());
        Assertions.assertEquals("isActive", opActiveGetter.get().getName());
    }

    @Test
    public void shouldReturnEmptyWhenGetter_MethodWithParams() {
        NamingResolver resolver = new DefaultNamingResolver();

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
        NamingResolver resolver = new DefaultNamingResolver();

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
        NamingResolver resolver = new DefaultNamingResolver();

        var opMethod = resolver.resolveSetter(TestEntity.class, "someField");

        Assertions.assertFalse(opMethod.isPresent());
    }

    @Test
    public void shouldReturnPublicField() {
        NamingResolver resolver = new DefaultNamingResolver();

        var opField = resolver.resolveField(TestEntity.class, "publicField");

        Assertions.assertTrue(opField.isPresent());
    }

    @Test
    public void shouldReturnEmpty_PrivateField() {
        NamingResolver resolver = new DefaultNamingResolver();

        var opField = resolver.resolveField(TestEntity.class, "someField");

        Assertions.assertFalse(opField.isPresent());
    }

    @Test
    public void shouldReturnEmpty_StaticGetter() {
        NamingResolver resolver = new DefaultNamingResolver();

        var opMethod = resolver.resolveGetter(TestEntity.class, "staticField");

        Assertions.assertFalse(opMethod.isPresent());
    }

    @Test
    public void shouldReturnEmpty_StaticSetter() {
        NamingResolver resolver = new DefaultNamingResolver();

        var opMethod = resolver.resolveSetter(TestEntity.class, "staticField");

        Assertions.assertFalse(opMethod.isPresent());
    }

    @Test
    public void shouldReturnEmpty_StaticPublicField() {
        NamingResolver resolver = new DefaultNamingResolver();

        var opMethod = resolver.resolveField(TestEntity.class, "staticField");

        Assertions.assertFalse(opMethod.isPresent());
    }
}
