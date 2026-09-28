package net.hostsharing.hsadminng.persistence;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.persistence.EntityManager;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static java.util.Map.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class EntityManagerWrapperUnitTest {
    private static final Map<Class<?>, Object> DEFAULT_VALUES = Map.ofEntries(
            entry(boolean.class, false),
            entry(byte.class, (byte) 0),
            entry(short.class, (short) 0),
            entry(int.class, 0),
            entry(long.class, 0L),
            entry(float.class, 0.0f),
            entry(double.class, 0.0),
            entry(char.class, '\0'),
            entry(String.class, "dummy"),
            entry(String[].class, new String[]{ "dummy" }),
            entry(Class.class, String.class),
            entry(Class[].class, new Class[0]));

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

    private Object getDefaultValue(final Class<?> type) {
        val predefined = DEFAULT_VALUES.get(type);
        if (predefined != null) {
            return predefined;
        }
        // Mockito cannot mock array types; a single element keeps a dropped varargs argument detectable
        if (type.isArray()) {
            val array = Array.newInstance(type.getComponentType(), 1);
            Array.set(array, 0, getDefaultValue(type.getComponentType()));
            return array;
        }
        return mock(type);
    }
}
