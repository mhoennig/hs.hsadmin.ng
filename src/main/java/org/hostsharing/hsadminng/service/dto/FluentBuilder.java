package org.hostsharing.hsadminng.service.dto;

import java.util.function.Consumer;

public class FluentBuilder<T> {

    @SuppressWarnings("unchecked")
    public T with(
        Consumer<T> builderFunction) {
        builderFunction.accept((T) this);
        return (T) this;
    }


}
