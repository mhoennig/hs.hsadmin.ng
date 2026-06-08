package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacEntity;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

class HostingAssetEntitySaveProcessorUnitTest {

    // CLOUD_SERVER with no bookingItem and an unexpected RAM property, produces two known validation errors:
    // - "'CLOUD_SERVER:vm1234.bookingItem' must be of type CLOUD_SERVER but is null"
    // - "'CLOUD_SERVER:vm1234.config.RAM' is not expected but is set to '2000'"
    private static final HsHostingAssetRbacEntity GIVEN_INVALID_ASSET = HsHostingAssetRbacEntity.builder()
            .type(CLOUD_SERVER)
            .identifier("vm1234")
            .config(Map.ofEntries(entry("RAM", 2000)))
            .build();

    private final EntityManager em = mock(EntityManager.class);

    @Test
    void validateEntityThrowsWhenPreprocessEntityWasNotCalledFirst() {
        // given
        final var processor = new HostingAssetEntitySaveProcessor(em, GIVEN_INVALID_ASSET);

        // when
        final var thrown = catchThrowable(processor::validateEntity);

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("expected preprocessEntity but got validateEntity");
    }

    @Test
    void validateEntityIgnoringThrowsWhenPreprocessEntityWasNotCalledFirst() {
        // given
        final var processor = new HostingAssetEntitySaveProcessor(em, GIVEN_INVALID_ASSET);

        // when
        final var thrown = catchThrowable(() -> processor.validateEntityIgnoring());

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("expected preprocessEntity but got validateEntity");
    }

    @Test
    void preprocessEntityThrowsWhenCalledTwice() {
        // given
        final var processor = new HostingAssetEntitySaveProcessor(em, GIVEN_INVALID_ASSET);
        processor.preprocessEntity();

        // when
        final var thrown = catchThrowable(processor::preprocessEntity);

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("expected validateEntity but got preprocessEntity");
    }

    @Test
    void validateEntityIgnoringWithNoPatternThrowsForAllErrors() {
        // given
        final var processor = new HostingAssetEntitySaveProcessor(em, GIVEN_INVALID_ASSET)
                .preprocessEntity();

        // when
        final var thrown = catchThrowable(processor::validateEntityIgnoring);

        // then
        assertThat(thrown).hasMessageContaining("bookingItem").hasMessageContaining("RAM");
    }

    @Test
    void validateEntityIgnoringFiltersOutMatchedErrors() {
        // given
        final var processor = new HostingAssetEntitySaveProcessor(em, GIVEN_INVALID_ASSET)
                .preprocessEntity();

        // when
        final var thrown = catchThrowable(() -> processor.validateEntityIgnoring(".*bookingItem.*"));

        // then
        assertThat(thrown).hasMessageContaining("RAM");
        assertThat(thrown.getMessage()).doesNotContain("bookingItem");
    }

    @Test
    void validateEntityIgnoringWithAllMatchingPatternsDoesNotThrow() {
        // given
        final var processor = new HostingAssetEntitySaveProcessor(em, GIVEN_INVALID_ASSET)
                .preprocessEntity();

        // when
        final var thrown = catchThrowable(() -> processor.validateEntityIgnoring(".*bookingItem.*", ".*RAM.*"));

        // then
        assertThat(thrown).isNull();
    }

    @Test
    void validateEntityIgnoringRequiresFullPatternMatch() {
        // given
        final var processor = new HostingAssetEntitySaveProcessor(em, GIVEN_INVALID_ASSET)
                .preprocessEntity();

        // when — "bookingItem" without wildcards does not match the full error string via matches()
        final var thrown = catchThrowable(() -> processor.validateEntityIgnoring("bookingItem"));

        // then — neither error is filtered
        assertThat(thrown).hasMessageContaining("bookingItem").hasMessageContaining("RAM");
    }
}
