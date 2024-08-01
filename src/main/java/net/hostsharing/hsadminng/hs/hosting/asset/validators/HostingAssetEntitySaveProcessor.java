package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.errors.MultiValidationException;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetResource;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;

import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.function.Function;

/**
 * Wraps the steps of the pararation, validation, mapping and revamp around saving of a HsHostingAsset into a readable API.
 */
public class HostingAssetEntitySaveProcessor {

    private final HsEntityValidator<HsHostingAsset> validator;
    private String expectedStep = "preprocessEntity";
    private final EntityManager em;
    private HsHostingAsset entity;
    private HsHostingAssetResource resource;

    public HostingAssetEntitySaveProcessor(final EntityManager em, final HsHostingAsset entity) {
        this.em = em;
        this.entity = entity;
        this.validator = HostingAssetEntityValidatorRegistry.forType(entity.getType());
    }

    /// initial step allowing to set default values before any validations
    public HostingAssetEntitySaveProcessor preprocessEntity() {
        step("preprocessEntity", "validateEntity");
        validator.preprocessEntity(entity);
        return this;
    }

    /// validates the entity itself including its properties
    public HostingAssetEntitySaveProcessor validateEntity() {
        step("validateEntity", "prepareForSave");
        MultiValidationException.throwIfNotEmpty(validator.validateEntity(entity));
        return this;
    }

    /// validates the entity itself including its properties, but ignoring some error messages for import of legacy data
    public HostingAssetEntitySaveProcessor validateEntityIgnoring(final String ignoreRegExp) {
        step("validateEntity", "prepareForSave");
        MultiValidationException.throwIfNotEmpty(
                validator.validateEntity(entity).stream()
                        .filter(errorMsg -> !errorMsg.matches(ignoreRegExp))
                        .toList()
        );
        return this;
    }

    /// hashing passwords etc.
    @SuppressWarnings("unchecked")
    public HostingAssetEntitySaveProcessor prepareForSave() {
        step("prepareForSave", "saveUsing");
        validator.prepareProperties(em, entity);
        return this;
    }

    public HostingAssetEntitySaveProcessor saveUsing(final Function<HsHostingAsset, HsHostingAsset> saveFunction) {
        step("saveUsing", "validateContext");
        entity = saveFunction.apply(entity);
        return this;
    }

    /// validates the entity within it's parent and child hierarchy (e.g. totals validators and other limits)
    public HostingAssetEntitySaveProcessor validateContext() {
        step("validateContext", "mapUsing");
        MultiValidationException.throwIfNotEmpty(validator.validateContext(entity));
        return this;
    }

    /// maps entity to JSON resource representation
    public HostingAssetEntitySaveProcessor mapUsing(
            final Function<HsHostingAsset, HsHostingAssetResource> mapFunction) {
        step("mapUsing", "revampProperties");
        resource = mapFunction.apply(entity);
        return this;
    }

    /// removes write-only-properties and ads computed-properties
    @SuppressWarnings("unchecked")
    public HsHostingAssetResource revampProperties() {
        step("revampProperties", null);
        final var revampedProps = validator.revampProperties(em, entity, (Map<String, Object>) resource.getConfig());
        resource.setConfig(revampedProps);
        return resource;
    }

    // Makes sure that the steps are called in the correct order.
    // Could also be implemented using an interface per method, but that seems exaggerated.
    private void step(final String current, final String next) {
        if (!expectedStep.equals(current)) {
            throw new IllegalStateException("expected " + expectedStep + " but got " + current);
        }
        expectedStep = next;
    }
}
