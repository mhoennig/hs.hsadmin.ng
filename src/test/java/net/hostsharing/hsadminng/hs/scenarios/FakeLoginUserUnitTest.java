package net.hostsharing.hsadminng.hs.scenarios;

import org.junit.jupiter.api.Test;

import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asSubject;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asSubjectWithGroups;
import static org.assertj.core.api.Assertions.assertThat;

class FakeLoginUserUnitTest {

    @Test
    void reportsBearerWithSubjectWithoutGroup() {
        assertThat(asSubject("some-user@example.org").reportableBearer())
                .isEqualTo("Bearer JWT {\"sub\":\"uuid<some-user@example.org>\"}");
    }

    @Test
    void reportsBearerWithSubjectAndGroups() {
        assertThat(asSubjectWithGroups("some-user@example.org", "/xyz-GroupOne", "/xyz-GroupTwo")
                .reportableBearer())
                .isEqualTo("Bearer JWT {\"sub\":\"uuid<some-user@example.org>\",\"groups\":[\"/xyz-GroupOne\",\"/xyz-GroupTwo\"]}");
    }

    @Test
    void reportsBearerWithSubjectCommentAndGroups() {
        assertThat(asSubject("some-user@example.org")
                .whichIs("a Debitor-Admin, here concretely the Partner-Representative")
                .withGroups("/xyz-GroupOne")
                .reportableBearer())
                .isEqualTo("Bearer JWT {\"comment\":\"a Debitor-Admin, here concretely the Partner-Representative\",\"sub\":\"uuid<some-user@example.org>\",\"groups\":[\"/xyz-GroupOne\"]}");
    }
}
