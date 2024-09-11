package net.hostsharing.hsadminng.persistence;

import net.hostsharing.hsadminng.mapper.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.persistence.EntityManager;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class EntityManagerWrapperUnitTest {
    private EntityManagerWrapper wrapper;
    private EntityManager delegateMock;

    @BeforeEach
    public void setUp() {
        delegateMock = mock(EntityManager.class);
        wrapper = new EntityManagerWrapper(delegateMock);
    }

    @Test
    public void testAllMethodsAreForwarded() throws Exception {
        final var methods = EntityManager.class.getMethods();

        for (Method method : methods) {
            // given prepared dummy arguments (if any)
            final var args = Arrays.stream(method.getParameterTypes())
                    .map(this::getDefaultValue)
                    .toArray();

            // when
            method.invoke(wrapper, args);

            // then verify that the same method was called on the mock delegate
            Mockito.verify(delegateMock, times(1)).getClass()
                    .getMethod(method.getName(), method.getParameterTypes())
                    .invoke(delegateMock, args);
        }
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0;
        if (type == char.class) return '\0';
        if (type == String.class) return "dummy";
        if (type == String[].class) return Array.of("dummy");
        if (type == Class.class) return String.class;
        if (type == Class[].class) return Array.of(String.class);
        return mock(type);
    }
}
