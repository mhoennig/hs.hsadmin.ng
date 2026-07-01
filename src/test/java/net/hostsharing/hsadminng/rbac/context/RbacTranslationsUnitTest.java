package net.hostsharing.hsadminng.rbac.context;

import net.hostsharing.hsadminng.config.MessageTranslator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacTranslationsUnitTest {

    @Mock
    private MessageTranslator messageTranslator;

    @InjectMocks
    private RbacTranslations rbacTranslations;

    @Test
    void canTranslateSubjectCannotAssumeRoleMessage() {
        assertThat(rbacTranslations.canTranslate(
                "ERROR: [403] subject tst-drew_selfregistered has no permission to assume role rbac.global#global:ADMIN"))
                .isTrue();
    }

    @Test
    void canTranslateUserSubjectNameCheckViolation() {
        assertThat(rbacTranslations.canTranslate("""
                ERROR: new row for relation "subject" violates check constraint "check_valid_user_subject_name"
                  Detail: Failing row contains (9786027a-d862-4387-8d94-ed45c3d05117, invalid-user@example.com, USER).
                """)).isTrue();
    }

    @Test
    void canTranslateGroupSubjectNameCheckViolation() {
        assertThat(rbacTranslations.canTranslate("""
                ERROR: new row for relation "subject" violates check constraint "check_valid_group_subject_name"
                  Detail: Failing row contains (9786027a-d862-4387-8d94-ed45c3d05117, xyz-Team, GROUP).
                """)).isTrue();
    }

    @Test
    void cannotTranslateOtherMessages() {
        assertThat(rbacTranslations.canTranslate("ERROR: [403] whatever")).isFalse();
    }

    @Test
    void translatesSubjectCannotAssumeRoleMessage() {
        // given
        final var message =
                "ERROR: [403] subject tst-drew_selfregistered has no permission to assume role rbac.global#global:ADMIN";
        when(messageTranslator.translate(
                "rbac.subject-{0}-has-no-permisson-to-assume-role-{1}",
                "tst-drew_selfregistered",
                "rbac.global#global:ADMIN"))
                .thenReturn("translated message");

        // when
        final var translatedMessage = rbacTranslations.translate(message);

        // then
        assertThat(translatedMessage).isEqualTo("ERROR: [403] translated message");
        verify(messageTranslator).translate(
                "rbac.subject-{0}-has-no-permisson-to-assume-role-{1}",
                "tst-drew_selfregistered",
                "rbac.global#global:ADMIN");
    }

    @Test
    void translatesUserSubjectNameCheckViolation() {
        // given
        final var message = """
                ERROR: new row for relation "subject" violates check constraint "check_valid_user_subject_name"
                  Detail: Failing row contains (9786027a-d862-4387-8d94-ed45c3d05117, invalid-user@example.com, USER).
                """;
        when(messageTranslator.translate(
                "rbac.user-subject-name-{0}-does-not-match-required-pattern",
                "invalid-user@example.com"))
                .thenReturn("translated message");

        // when
        final var translatedMessage = rbacTranslations.translate(message);

        // then
        assertThat(translatedMessage).isEqualTo("ERROR: [400] translated message");
        verify(messageTranslator).translate(
                "rbac.user-subject-name-{0}-does-not-match-required-pattern",
                "invalid-user@example.com");
    }

    @Test
    void translatesGroupSubjectNameCheckViolation() {
        // given
        final var message = """
                ERROR: new row for relation "subject" violates check constraint "check_valid_group_subject_name"
                  Detail: Failing row contains (9786027a-d862-4387-8d94-ed45c3d05117, xyz-Team, GROUP).
                """;
        when(messageTranslator.translate(
                "rbac.group-subject-name-{0}-does-not-match-required-pattern",
                "xyz-Team"))
                .thenReturn("translated message");

        // when
        final var translatedMessage = rbacTranslations.translate(message);

        // then
        assertThat(translatedMessage).isEqualTo("ERROR: [400] translated message");
        verify(messageTranslator).translate(
                "rbac.group-subject-name-{0}-does-not-match-required-pattern",
                "xyz-Team");
    }
}
