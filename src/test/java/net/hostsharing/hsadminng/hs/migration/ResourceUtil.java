package net.hostsharing.hsadminng.hs.migration;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.opentest4j.AssertionFailedError;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.FileCopyUtils.copyToByteArray;

@UtilityClass
public class ResourceUtil {

    public void assertResourceHash(final String givenResourceOrFileName, final String expectedHash) throws IOException,
            NoSuchAlgorithmException {
        try (val inputStream = resourceOf(givenResourceOrFileName).getInputStream()) {
            val fileContent = copyToByteArray(inputStream);
            val digest = MessageDigest.getInstance("SHA-256");
            val hashBytes = digest.digest(fileContent);
            val hashHex = bytesToHex(hashBytes);
            assertThat(hashHex).isEqualTo(expectedHash);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        val result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static @NotNull AbstractResource resourceOf(final String sqlFile) {
        return new File(sqlFile).exists()
                ? new FileSystemResource(sqlFile)
                : new ClassPathResource(sqlFile);
    }

    public static Reader resourceReader(@NotNull final String resourcePath) {
        try {
            return new InputStreamReader(requireNonNull(resourceOf(resourcePath).getInputStream()));
        } catch (final Exception exc) {
            throw new AssertionFailedError("cannot open '" + resourcePath + "'");
        }
    }

    @SneakyThrows
    public static String resourceAsString(final Resource resource) {
        final var lines = Files.readAllLines(resource.getFile().toPath(), StandardCharsets.UTF_8);
        return String.join("\n", lines);
    }
}
