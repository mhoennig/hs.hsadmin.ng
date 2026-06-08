package net.hostsharing.hsadminng.hs.office.coopshares;

import lombok.val;

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
class HsCoopShareTranslationsUnitTest {

    private static final String NEGATIVE_SHARES_MESSAGE =
            "ERROR: [400] office.coop-shares.transaction-would-result-in-a-negative-number-of-shares";

    @Mock
    private MessageTranslator messageTranslator;

    @InjectMocks
    private HsCoopShareTranslations translations;

    @Test
    void canTranslateNegativeSharesMessage() {
        assertThat(translations.canTranslate(NEGATIVE_SHARES_MESSAGE)).isTrue();
    }

    @Test
    void cannotTranslateOtherMessages() {
        assertThat(translations.canTranslate("ERROR: [400] whatever")).isFalse();
    }

    @Test
    void translatesNegativeSharesMessage() {
        // given
        when(messageTranslator.translate("office.coop-shares.transaction-would-result-in-a-negative-number-of-shares"))
                .thenReturn("translated message");

        // when
        val translatedMessage = translations.translate(NEGATIVE_SHARES_MESSAGE);

        // then
        assertThat(translatedMessage).isEqualTo("ERROR: [400] translated message");
        verify(messageTranslator).translate("office.coop-shares.transaction-would-result-in-a-negative-number-of-shares");
    }
}
