package net.hostsharing.hsadminng.errors;

import net.hostsharing.hsadminng.config.MessageTranslator;

import java.util.UUID;

import static java.util.Locale.ENGLISH;

public class ReferenceNotFoundException extends RuntimeException {

    private final String TRANSLATABLE_MESSAGE = "{0} \"{1}\" not found";

    private final MessageTranslator translator;

    private final Class<?> entityClass;
    private final String entityClassDisplayName;
    private final UUID uuid;

    public <E> ReferenceNotFoundException(final MessageTranslator translator, final Class<E> entityClass, final UUID uuid, final Throwable exc) {
        super(exc);
        this.translator = translator;
        this.entityClass = entityClass;
        this.entityClassDisplayName = DisplayAs.DisplayName.of(entityClass);
        this.uuid = uuid;
    }

    @Override
    public String getMessage() {
        return translator.translateTo(ENGLISH, TRANSLATABLE_MESSAGE, entityClassDisplayName, uuid);
    }

    @Override
    public String getLocalizedMessage() {
        return translator.translate(TRANSLATABLE_MESSAGE, entityClassDisplayName, uuid);
    }
}
