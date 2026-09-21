package net.hostsharing.hsadminng.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.persistence.EntityManager;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
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
            // given prepared dummy arguments and stubbed return value (kills "replaced return value" mutants)
            final var args = Arrays.stream(method.getParameterTypes())
                    .map(this::getDefaultValue)
                    .toArray();
            final var returnType = method.getReturnType();
            final var isVoid = returnType == void.class;
            if (isVoid) {
                method.invoke(wrapper, args);
                method.invoke(Mockito.verify(delegateMock, times(1)), args);
            } else if (returnType == boolean.class) {
                // boolean has two mutants (false/true) — need both values to kill them
                for (boolean expected : new boolean[]{false, true}) {
                    final var boolDelegate = mock(EntityManager.class);
                    final var boolWrapper = new EntityManagerWrapper(boolDelegate);
                    Mockito.lenient().when(method.invoke(boolDelegate, args)).thenReturn(expected);
                    final var actual = (boolean) method.invoke(boolWrapper, args);
                    assertThat(actual).as("return value of %s", method.getName()).isEqualTo(expected);
                    method.invoke(Mockito.verify(boolDelegate, times(1)), args);
                }
            } else {
                final var expected = getDefaultValue(returnType);
                Mockito.lenient().when(method.invoke(delegateMock, args)).thenReturn(expected);
                final var actual = method.invoke(wrapper, args);
                assertThat(actual).as("return value of %s", method.getName()).isEqualTo(expected);
                method.invoke(Mockito.verify(delegateMock, times(1)), args);
            }
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
        if (type == String[].class) return new String[]{"dummy"};
        if (type == Class.class) return String.class;
        if (type == Class[].class) return new Class[0];
        return mock(type);
    }
}
