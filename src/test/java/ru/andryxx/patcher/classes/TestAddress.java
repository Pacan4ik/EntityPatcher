package ru.andryxx.patcher.classes;

public class TestAddress {
    private String city;
    private int zipCode;

    public TestAddress() {}

    public TestAddress(String city, int zipCode) {
        this.city = city;
        this.zipCode = zipCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getZipCode() {
        return zipCode;
    }

    public void setZipCode(int zipCode) {
        this.zipCode = zipCode;
    }
}
