package ru.andryxx.classes;

import java.time.LocalDate;
import java.util.Objects;

public class TestEntity {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TestEntity that)) return false;
        return age == that.age && active == that.active && Objects.equals(name, that.name) && Objects.equals(address, that.address) && Objects.equals(birthdate, that.birthdate) && Objects.equals(someField, that.someField) && Objects.equals(publicField, that.publicField) && Objects.equals(publicObjectField, that.publicObjectField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age, active, address, birthdate, someField, publicField, publicObjectField);
    }

    private String name;
    private int age;
    private boolean active;
    private TestAddress address;
    private LocalDate birthdate;

    private String someField;
    public String publicField;
    public Object publicObjectField;

    public static String staticField;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }


    public void setAge(int age) {
        this.age = age;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public TestAddress getAddress() {
        return address;
    }

    public void setAddress(TestAddress address) {
        this.address = address;
    }

    public int getSomething(int a) {
        return a;
    }

    private void setSomeField(String someField) {
        this.someField = someField;
    }

    public static String getStaticField() {
        return staticField;
    }

    public static void setStaticField(String staticField) {
        TestEntity.staticField = staticField;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }
}
