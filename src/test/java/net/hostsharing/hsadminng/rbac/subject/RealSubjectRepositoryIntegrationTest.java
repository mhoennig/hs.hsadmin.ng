package net.hostsharing.hsadminng.rbac.subject;

import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.servlet.http.HttpServletRequest;

import static net.hostsharing.hsadminng.rbac.subject.SubjectType.GROUP;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(Context.class)
@Tag("generalIntegrationTest")
class RealSubjectRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    RealSubjectRepository realSubjectRepository;

    @MockitoBean
    HttpServletRequest request;

    @Nested
    class FindCurrentSubjectGroupSubjectsByOptionalNameLike {

        @Test
        void returnsGroupSubjectFromCurrentSubjectGroups() {

            // given
            contextWithGroups("/xyz-Service");

            // when
            final var result = realSubjectRepository.findCurrentSubjectGroupSubjectsByOptionalNameLike(
                    "/xyz-Service");

            // then
            assertThat(result)
                    .extracting(RealSubjectEntity::getName)
                    .containsExactly("/xyz-Service");
            assertThat(result)
                    .extracting(RealSubjectEntity::getType)
                    .containsOnly(GROUP);
        }

        @Test
        void returnsFlatGroupSubjectsFromCurrentSubjectGroups() {

            // given
            contextWithGroups("/xyz-Team;/xyz-Service");

            // when
            final var result = realSubjectRepository.findCurrentSubjectGroupSubjectsByOptionalNameLike(
                    "/xyz");

            // then
            assertThat(result)
                    .extracting(RealSubjectEntity::getName)
                    .containsExactly(
                            "/xyz-Service",
                            "/xyz-Team");
        }

        private void contextWithGroups(final String currentSubjectGroups) {
            context.define(
                    "RealSubjectRepositoryIntegrationTest",
                    null,
                    "tst-person_firbysusan",
                    null,
                    currentSubjectGroups);
        }
    }
}
