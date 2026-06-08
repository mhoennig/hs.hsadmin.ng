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
                "ERROR: [403] subject selfregistered-user-drew@hostsharing.org has no permission to assume role rbac.global#global:ADMIN"))
                .isTrue();
    }

    @Test
    void cannotTranslateOtherMessages() {
        assertThat(rbacTranslations.canTranslate("ERROR: [403] whatever")).isFalse();
    }

    @Test
    void translatesSubjectCannotAssumeRoleMessage() {
        // given
        final var message =
                "ERROR: [403] subject selfregistered-user-drew@hostsharing.org has no permission to assume role rbac.global#global:ADMIN";
        when(messageTranslator.translate(
                "rbac.subject-{0}-has-no-permisson-to-assume-role-{1}",
                "selfregistered-user-drew@hostsharing.org",
                "rbac.global#global:ADMIN"))
                .thenReturn("translated message");

        // when
        final var translatedMessage = rbacTranslations.translate(message);

        // then
        assertThat(translatedMessage).isEqualTo("ERROR: [403] translated message");
        verify(messageTranslator).translate(
                "rbac.subject-{0}-has-no-permisson-to-assume-role-{1}",
                "selfregistered-user-drew@hostsharing.org",
                "rbac.global#global:ADMIN");
    }
}
