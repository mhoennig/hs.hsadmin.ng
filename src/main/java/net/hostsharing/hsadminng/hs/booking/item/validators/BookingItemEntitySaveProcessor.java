package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.errors.MultiValidationException;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingItemResource;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItem;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;

import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

// TODO.refa: introduce common base class with HsHostingAssetEntitySaveProcessor
/**
 * Wraps the steps of the pararation, validation, mapping and revamp around saving of a HsBookingItem into a readable API.
 */
public class BookingItemEntitySaveProcessor {

    private final HsEntityValidator<HsBookingItem> validator;
    private String expectedStep = "preprocessEntity";
    private final EntityManager em;
    private HsBookingItem entity;
    private HsBookingItemResource resource;

    public BookingItemEntitySaveProcessor(final EntityManager em, final HsBookingItem entity) {
        this.em = em;
        this.entity = entity;
        this.validator = HsBookingItemEntityValidatorRegistry.forType(entity.getType());
    }

    /// initial step allowing to set default values before any validations
    public BookingItemEntitySaveProcessor preprocessEntity() {
        step("preprocessEntity", "validateEntity");
        validator.preprocessEntity(entity);
        return this;
    }

    /// validates the entity itself including its properties
    public BookingItemEntitySaveProcessor validateEntity() {
        step("validateEntity", "prepareForSave");
        MultiValidationException.throwIfNotEmpty(validator.validateEntity(entity));
        return this;
    }

    // TODO.impl: remove once the migration of legacy data is done
    /// validates the entity itself including its properties, but ignoring some error messages for import of legacy data
    public BookingItemEntitySaveProcessor validateEntityIgnoring(final String... ignoreRegExp) {
        step("validateEntity", "prepareForSave");
        final var ignoreRegExpPatterns = Arrays.stream(ignoreRegExp).map(Pattern::compile).toList();
        MultiValidationException.throwIfNotEmpty(
                validator.validateEntity(entity).stream()
                        .filter(error -> ignoreRegExpPatterns.stream().noneMatch(p -> p.matcher(error).matches() ))
                        .toList()
        );
        return this;
    }

    /// hashing passwords etc.
    public BookingItemEntitySaveProcessor prepareForSave() {
        step("prepareForSave", "save");
        validator.prepareProperties(em, entity);
        return this;
    }

    /**
     * Saves the entity using the given `saveFunction`.
     *
     * <p>`validator.postPersist(em, entity)` is NOT called.
     * If any postprocessing is necessary, the saveFunction has to implement this.</p>
     * @param saveFunction
     * @return this
     */
    public BookingItemEntitySaveProcessor saveUsing(final Function<HsBookingItem, HsBookingItem> saveFunction) {
        step("save", "validateContext");
        entity = saveFunction.apply(entity);
        return this;
    }

    /**
     * Saves the using the `EntityManager`, but does NOT ever merge the entity.
     *
     * <p>`validator.postPersist(em, entity)` is called afterwards with the entity guaranteed to be flushed to the database.</p>
     * @return this
     */
    public BookingItemEntitySaveProcessor save() {
        return saveUsing(e -> {
            if (!em.contains(entity)) {
                em.persist(entity);
            }
            em.flush(); // makes RbacEntity available as RealEntity if needed
            validator.postPersist(em, entity);
            return entity;
        });
    }

    /// validates the entity within it's parent and child hierarchy (e.g. totals validators and other limits)
    public BookingItemEntitySaveProcessor validateContext() {
        step("validateContext", "mapUsing");
        return HsEntityValidator.doWithEntityManager(em, () -> {
            MultiValidationException.throwIfNotEmpty(validator.validateContext(entity));
            return this;
        });
    }

    /// maps entity to JSON resource representation
    public BookingItemEntitySaveProcessor mapUsing(
            final Function<HsBookingItem, HsBookingItemResource> mapFunction) {
        step("mapUsing", "revampProperties");
        resource = mapFunction.apply(entity);
        return this;
    }

    /// removes write-only-properties and ads computed-properties
    @SuppressWarnings("unchecked")
    public HsBookingItemResource revampProperties() {
        step("revampProperties", null);
        final var revampedProps = validator.revampProperties(em, entity, (Map<String, Object>) resource.getResources());
        resource.setResources(revampedProps);
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
