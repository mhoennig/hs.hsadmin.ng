package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.errors.MultiValidationException;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetResource;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;

import java.util.Map;
import java.util.function.Function;

/**
 * Wraps the steps of the pararation, validation, mapping and revamp around saving of a HsHostingAssetEntity into a readable API.
 */
public class HsHostingAssetEntityProcessor {

    private final HsEntityValidator<HsHostingAssetEntity> validator;
    private String expectedStep = "preprocessEntity";
    private HsHostingAssetEntity entity;
    private HsHostingAssetResource resource;

    public HsHostingAssetEntityProcessor(final HsHostingAssetEntity entity) {
        this.entity = entity;
        this.validator = HsHostingAssetEntityValidatorRegistry.forType(entity.getType());
    }

    /// initial step allowing to set default values before any validations
    public HsHostingAssetEntityProcessor preprocessEntity() {
        step("preprocessEntity", "validateEntity");
        validator.preprocessEntity(entity);
        return this;
    }

    /// validates the entity itself including its properties
    public HsHostingAssetEntityProcessor validateEntity() {
        step("validateEntity", "prepareForSave");
        MultiValidationException.throwIfNotEmpty(validator.validateEntity(entity));
        return this;
    }

    /// hashing passwords etc.
    @SuppressWarnings("unchecked")
    public  HsHostingAssetEntityProcessor prepareForSave() {
        step("prepareForSave", "saveUsing");
        validator.prepareProperties(entity);
        return this;
    }

    public HsHostingAssetEntityProcessor saveUsing(final Function<HsHostingAssetEntity, HsHostingAssetEntity> saveFunction) {
        step("saveUsing", "validateContext");
        entity = saveFunction.apply(entity);
        return this;
    }

    /// validates the entity within it's parent and child hierarchy (e.g. totals validators and other limits)
    public HsHostingAssetEntityProcessor validateContext() {
        step("validateContext", "mapUsing");
        MultiValidationException.throwIfNotEmpty(validator.validateContext(entity));
        return this;
    }

    /// maps entity to JSON resource representation
    public HsHostingAssetEntityProcessor mapUsing(
            final Function<HsHostingAssetEntity, HsHostingAssetResource> mapFunction) {
        step("mapUsing", "revampProperties");
        resource = mapFunction.apply(entity);
        return this;
    }

    /// removes write-only-properties and ads computed-properties
    @SuppressWarnings("unchecked")
    public HsHostingAssetResource revampProperties() {
        step("revampProperties", null);
        final var revampedProps = validator.revampProperties(entity, (Map<String, Object>) resource.getConfig());
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
