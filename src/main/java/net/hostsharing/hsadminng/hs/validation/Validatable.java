package net.hostsharing.hsadminng.hs.validation;


import java.util.Map;

public interface Validatable<E, T extends Enum<T>> {


    Enum<T> getType();

    String getPropertiesName();
    Map<String, Object> getProperties();
}
