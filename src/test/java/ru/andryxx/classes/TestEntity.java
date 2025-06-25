package ru.andryxx.classes;

public class TestEntity {
    private String name;
    private int age;
    private boolean active;
    private TestAddress address;

    private String someField;
    public String publicField;

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
}
