package org.hostsharing.hsadminng.service.dto;

import java.util.function.Consumer;

/**
 * Just 'implement' this interface in your class to get a pseudo fluent builder, no more code needed.
 *
 * @param <T> class to be build (same as to which the interface was added)
 */
public interface FluentBuilder<T> {

    /**
     * Allows statements on the target instance possible as expression.
     *
     * This allows creating nested object structures, e.g. for test data
     * in a much more readable way.
     *
     * <h3>Example</h3>
     * {code
     *      // adding a fluent builder to your class
     *      class YourClass implements FluentBuilder<YourClass> {
     *          public int someField;
     *          public String anotherField;
     *          // ...
     *      }
     *
     *      // using the fluent builder somewhere else
     *      someMethod( new YourClass().with( it -> {
     *          it.someField = 5;
     *          it.anotherField = "Hello";
     *      }));
     * }
     *
     * @param builderFunction statements to apply to 'this'
     *
     * @return the instance on which 'with(...)' was executed.
     */
    @SuppressWarnings("unchecked")
    default T with(
        Consumer<T> builderFunction) {
        builderFunction.accept((T) this);
        return (T) this;
    }


}
