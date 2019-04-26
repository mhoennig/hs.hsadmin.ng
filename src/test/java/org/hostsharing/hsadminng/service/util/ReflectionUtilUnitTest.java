package org.hostsharing.hsadminng.service.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.hostsharing.hsadminng.service.util.ReflectionUtil.unchecked;


public class ReflectionUtilUnitTest {

    @Test
    public void setValue() {
        final TestDto dto = new TestDto(5);
        ReflectionUtil.setValue(dto, ReflectionUtil.getField(dto.getClass(), "intVal"), 77);
        assertThat(dto.intVal).isEqualTo(77);
    }

    @Test
    public void setValueViaSuperclass() {
        final SubTestDto dto = new SubTestDto(5);
        ReflectionUtil.setValue(dto, ReflectionUtil.getField(dto.getClass(), "intVal"), 77);
        assertThat(dto.intVal).isEqualTo(77);
    }

    @Test
    public void getValue() {
        final TestDto dto = new TestDto(5);
        final int actual = ReflectionUtil.getValue(dto, ReflectionUtil.getField(dto.getClass(), "intVal"));
        assertThat(actual).isEqualTo(5);
    }

    @Test
    public void getValueViaSuperclass() {
        final SubTestDto dto = new SubTestDto(5);
        final int actual = ReflectionUtil.getValue(dto, ReflectionUtil.getField(dto.getClass(), "intVal"));
        assertThat(actual).isEqualTo(5);
    }

    @Test
    public void determineGenericInterfaceParameteDirect() {
        Class<?> actual = ReflectionUtil.determineGenericInterfaceParameter(SuperClass.class, GenericInterface.class, 1);
        assertThat(actual).isEqualTo(Long.class);
    }

    @Test
    public void determineGenericInterfaceParameterViaSuperclass() {
        Class<?> actual = ReflectionUtil.determineGenericInterfaceParameter(SomeClass.class, GenericInterface.class, 1);
        assertThat(actual).isEqualTo(Long.class);
    }

    @Test
    public void throwsExceptionIfGenericInterfaceNotImplemented() {
        final Throwable actual = catchThrowable(() -> ReflectionUtil.determineGenericInterfaceParameter(SomeClass.class, UnusedGenericInterface.class, 1));
        assertThat(actual).isInstanceOf(AssertionError.class).hasMessageContaining("SomeClass expected to implement UnusedGenericInterface<...>");
    }

    @Test
    public void determineGenericClassParameterDirect() {
        Class<?> actual = ReflectionUtil.determineGenericClassParameter(SuperClass.class, GenericClass.class, 1);
        assertThat(actual).isEqualTo(Boolean.class);
    }

    @Test
    public void determineGenericClassParameterViaSuperclss() {
        Class<?> actual = ReflectionUtil.determineGenericClassParameter(SomeClass.class, GenericClass.class, 1);
        assertThat(actual).isEqualTo(Boolean.class);
    }

    @Test
    public void throwsExceptionIfGenericClassNotExended() {
        final Throwable actual = catchThrowable(() -> ReflectionUtil.determineGenericClassParameter(SomeClass.class, UnusedSuperClass.class, 1));
        assertThat(actual).isInstanceOf(AssertionError.class).hasMessageContaining("GenericClass expected to extend UnusedSuperClass<...>");
    }

    @Test
    public void uncheckedRethrowsCheckedException() {
        final Exception givenException = new Exception("Checked Test Exception");
        final Throwable actual = catchThrowable(() -> unchecked(() -> {
            throw givenException;
        }));
        assertThat(actual).isInstanceOfSatisfying(RuntimeException.class, rte ->
            assertThat(rte.getCause()).isSameAs(givenException)
        );
    }

    @Test
    public void asEnumValue() {
        assertThat(ReflectionUtil.asEnumValue(SomeEnum.class, "GREEN")).isEqualTo(SomeEnum.GREEN);
    }

    // --- only test fixture below ---

    private static class TestDto {
        int intVal;

        TestDto(final int intval) {
            this.intVal = intval;
        }
    }

    private static class SubTestDto extends TestDto {

        SubTestDto(final int intval) {
            super(intval);
        }
    }

    private static class SomeClass extends SuperClass {
    }

    private static class SuperClass extends GenericClass<String, Boolean> implements IntermediateInterfaces<Integer, Long> {
    }

    private static class UnusedSuperClass extends GenericClass<String, Boolean> implements IntermediateInterfaces<Integer, Long> {
    }

    private static class GenericClass<T, V> {
    }

    private interface IntermediateInterfaces<T, V> extends GenericInterface<Integer, Long> {
    }

    private interface GenericInterface<T, V> {
    }

    private interface UnusedGenericInterface<T, V> {
    }

    enum SomeEnum {
        RED, BLUE, GREEN
    }
}
