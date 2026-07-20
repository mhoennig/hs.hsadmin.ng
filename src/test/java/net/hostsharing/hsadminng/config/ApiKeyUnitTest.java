package net.hostsharing.hsadminng.config;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyUnitTest {

    @Test
    void generateCreatesUniqueKeysWithEmbeddedSubjectName() {
        val apiKey1 = ApiKey.generate("master.key");
        val apiKey2 = ApiKey.generate("master.key");

        assertThat(apiKey1)
                .startsWith(ApiKey.PREFIX + "master.key.")
                .hasSize(ApiKey.PREFIX.length() + "master.key.".length() + 64)
                .isNotEqualTo(apiKey2);
    }

    @Test
    void subjectNameOfExtractsTheEmbeddedSubjectName() {
        val apiKey = ApiKey.generate("master.key");

        assertThat(ApiKey.subjectNameOf(apiKey)).contains("master.key");
    }

    @Test
    void subjectNameOfExtractsSubjectNamesContainingTheDelimiter() {
        // subject names may contain dots; the fixed-length random part keeps parsing unambiguous
        val apiKey = ApiKey.generate("my.key");

        assertThat(ApiKey.subjectNameOf(apiKey)).contains("my.key");
    }

    @Test
    void subjectNameOfIsEmptyForKeysWithoutEmbeddedName() {
        // legacy format: prefix + 64 hex chars, without an embedded subject name
        val legacyApiKey = ApiKey.PREFIX + "e4ef543d74c631ae770db9da1c06872b73ad32b0bed0f14712dedb77d2e11e9d";

        assertThat(ApiKey.subjectNameOf(legacyApiKey)).isEmpty();
        assertThat(ApiKey.subjectNameOf("hsak_short")).isEmpty();
        assertThat(ApiKey.subjectNameOf("no-prefix")).isEmpty();
    }

    @Test
    void hashCreatesLowerCaseHexEncodedSha256() {
        assertThat(ApiKey.hash("test"))
                .isEqualTo("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
    }
}
