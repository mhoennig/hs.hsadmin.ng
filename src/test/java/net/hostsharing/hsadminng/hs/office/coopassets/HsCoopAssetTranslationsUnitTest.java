package net.hostsharing.hsadminng.hs.office.coopassets;

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
class HsCoopAssetTranslationsUnitTest {

    private static final String NEGATIVE_ASSETS_MESSAGE =
            "ERROR: [400] office.coop-assets.transaction-would-result-in-a-negative-balance-of-assets";

    @Mock
    private MessageTranslator messageTranslator;

    @InjectMocks
    private HsCoopAssetTranslations translations;

    @Test
    void canTranslateNegativeAssetsMessage() {
        assertThat(translations.canTranslate(NEGATIVE_ASSETS_MESSAGE)).isTrue();
    }

    @Test
    void cannotTranslateOtherMessages() {
        assertThat(translations.canTranslate("ERROR: [400] whatever")).isFalse();
    }

    @Test
    void translatesNegativeAssetsMessage() {
        // given
        when(messageTranslator.translate("office.coop-assets.transaction-would-result-in-a-negative-balance-of-assets"))
                .thenReturn("translated message");

        // when
        val translatedMessage = translations.translate(NEGATIVE_ASSETS_MESSAGE);

        // then
        verify(messageTranslator).translate("office.coop-assets.transaction-would-result-in-a-negative-balance-of-assets");
        assertThat(translatedMessage).isEqualTo("ERROR: [400] translated message");
    }
}
