package net.hostsharing.hsadminng.ping;

import lombok.val;
import net.hostsharing.hsadminng.generated.api.v1.model.ApiVersionGetResponse200Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Properties;

@Component
public class VersionProvider {

    private final ApiVersionGetResponse200Resource versionInfo;

    public VersionProvider() {
        this.versionInfo = loadVersionInfo("/META-INF/build-info.properties", "/git.properties");
    }

    public ResponseEntity<ApiVersionGetResponse200Resource> getVersion() {
        return ResponseEntity.ok(versionInfo);
    }

    // package-private with resource paths as parameters for testability
    static ApiVersionGetResponse200Resource loadVersionInfo(final String buildInfoResource, final String gitPropertiesResource) {
        val resource = new ApiVersionGetResponse200Resource();
        try {
            val props = new Properties();
            props.load(VersionProvider.class.getResourceAsStream(buildInfoResource));

            resource.setVersion(props.getProperty("build.version"));
            resource.setGroup(props.getProperty("build.group"));
            resource.setArtifact(props.getProperty("build.artifact"));
            resource.setName(props.getProperty("build.name"));

            // build.time is only present when built with -PincludeBuildTime=true; otherwise stays null.
            val buildTime = props.getProperty("build.time");
            if (buildTime != null) {
                resource.setBuildTime(OffsetDateTime.parse(buildTime));
            }

            // build.host stays null for jars built before this property was introduced
            resource.setBuildHost(props.getProperty("build.host"));
        } catch (IOException | NullPointerException e) {
            resource.setVersion("unknown");
            resource.setGroup("unknown");
            resource.setArtifact("unknown");
            resource.setName("unknown");
        }

        // git.branch/git.commit.id stay null when the build had no git information (e.g. exported sources without .git).
        try {
            val gitProps = new Properties();
            gitProps.load(VersionProvider.class.getResourceAsStream(gitPropertiesResource));
            resource.setGitBranch(gitProps.getProperty("git.branch"));
            resource.setGitCommit(gitProps.getProperty("git.commit.id"));
            val gitDirty = gitProps.getProperty("git.dirty");
            if (gitDirty != null) {
                resource.setGitDirty(Boolean.parseBoolean(gitDirty));
            }
        } catch (IOException | NullPointerException e) {
            // no git.properties on the classpath: leave gitBranch, gitCommit, and gitDirty null
        }

        return resource;
    }
}
