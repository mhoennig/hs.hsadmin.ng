package net.hostsharing.hsadminng.ping;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class VersionProviderUnitTest {

    @Test
    void getVersionReturnsBuildInfoFromClasspath() {
        // given
        val versionProvider = new VersionProvider();

        // when
        val response = versionProvider.getVersion();

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        val body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getVersion()).isNotEmpty().isNotEqualTo("unknown");
        assertThat(body.getGroup()).isNotEmpty().isNotEqualTo("unknown");
        assertThat(body.getArtifact()).isNotEmpty().isNotEqualTo("unknown");
        assertThat(body.getName()).isNotEmpty().isNotEqualTo("unknown");
        // buildTime stays null unless built with -PincludeBuildTime=true => not asserted here
    }

    @Test
    void loadVersionInfoParsesOptionalBuildTimeAndGitInfo() {
        // when
        val versionInfo = VersionProvider.loadVersionInfo(
                "/version-provider/build-info-with-time.properties", "/version-provider/git.properties");

        // then
        assertThat(versionInfo.getVersion()).isEqualTo("1.2.3-TEST");
        assertThat(versionInfo.getGroup()).isEqualTo("net.hostsharing.test");
        assertThat(versionInfo.getArtifact()).isEqualTo("hsadmin-ng-test");
        assertThat(versionInfo.getName()).isEqualTo("hsadmin-ng test build");
        assertThat(versionInfo.getBuildTime()).isEqualTo(OffsetDateTime.parse("2026-07-11T06:00:00Z"));
        assertThat(versionInfo.getBuildHost()).isEqualTo("test-build-host");
        assertThat(versionInfo.getGitBranch()).isEqualTo("test-branch");
        assertThat(versionInfo.getGitCommit()).isEqualTo("0123456789abcdef0123456789abcdef01234567");
        assertThat(versionInfo.getGitDirty()).isTrue();
    }

    @Test
    void loadVersionInfoFallsBackToUnknownWithoutBuildInfoOnClasspath() {
        // when
        val versionInfo = VersionProvider.loadVersionInfo("/does-not-exist.properties", "/does-not-exist.properties");

        // then
        assertThat(versionInfo.getVersion()).isEqualTo("unknown");
        assertThat(versionInfo.getGroup()).isEqualTo("unknown");
        assertThat(versionInfo.getArtifact()).isEqualTo("unknown");
        assertThat(versionInfo.getName()).isEqualTo("unknown");
        assertThat(versionInfo.getBuildTime()).isNull();
        assertThat(versionInfo.getBuildHost()).isNull();
        assertThat(versionInfo.getGitBranch()).isNull();
        assertThat(versionInfo.getGitCommit()).isNull();
        assertThat(versionInfo.getGitDirty()).isNull();
    }
}
