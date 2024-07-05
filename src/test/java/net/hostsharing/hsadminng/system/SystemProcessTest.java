package net.hostsharing.hsadminng.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.condition.OS.LINUX;

class SystemProcessTest {

    @Test
    @EnabledOnOs(LINUX)
    void shouldExecuteAndFetchOutput() throws IOException, InterruptedException {
        // given
        final var process = new SystemProcess("bash", "-c", "echo 'Hello, World!'; echo 'Error!' >&2");

        // when
        final var returnCode = process.execute();

        // then
        assertThat(returnCode).isEqualTo(0);
        assertThat(process.getStdOut()).isEqualTo("Hello, World!\n");
        assertThat(process.getStdErr()).isEqualTo("Error!\n");
    }

    @Test
    @EnabledOnOs(LINUX)
    void shouldReturnErrorCode() throws IOException, InterruptedException {
        // given
        final var process = new SystemProcess("false");

        // when
        final int returnCode = process.execute();

        // then
        assertThat(returnCode).isEqualTo(1);
    }

    @Test
    @EnabledOnOs(LINUX)
    void shouldExecuteAndFeedInput() throws IOException, InterruptedException {
        // given
        final var process = new SystemProcess("tr", "[:lower:]", "[:upper:]");

        // when
        final int returnCode = process.execute("Hallo");

        // then
        assertThat(returnCode).isEqualTo(0);
        assertThat(process.getStdOut()).isEqualTo("HALLO\n");
    }

    @Test
    void shouldThrowExceptionIfProgramNotFound() {
        // given
        final var process = new SystemProcess("non-existing program");

        // when
        final var exception = catchThrowable(process::execute);

        // then
        assertThat(exception).isInstanceOf(IOException.class)
                .hasMessage("Cannot run program \"non-existing program\": error=2, No such file or directory");
    }

    @Test
    void shouldBeAbleToRunMultipleTimes() throws IOException, InterruptedException {
        // given
        final var process = new SystemProcess("true");

        // when
        process.execute();
        final int returnCode = process.execute();

        // then
        assertThat(returnCode).isEqualTo(0);
    }
}
