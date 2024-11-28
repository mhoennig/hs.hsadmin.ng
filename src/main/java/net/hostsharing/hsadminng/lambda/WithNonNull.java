package net.hostsharing.hsadminng.lambda;

import java.util.function.Consumer;

public class WithNonNull {
        public static <T> void withNonNull(final T target, final Consumer<T> code) {
            if (target != null ) {
                code.accept(target);
            }
    }
}
