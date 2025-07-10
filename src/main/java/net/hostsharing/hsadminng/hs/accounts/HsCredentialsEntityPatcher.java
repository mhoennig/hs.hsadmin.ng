package net.hostsharing.hsadminng.hs.accounts;

import net.hostsharing.hsadminng.accounts.generated.api.v1.model.CredentialsPatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;


public class HsCredentialsEntityPatcher implements EntityPatcher<CredentialsPatchResource> {

    private CredentialContextResourceToEntityMapper contextMapper;
    private final HsCredentialsEntity entity;

    public HsCredentialsEntityPatcher(final CredentialContextResourceToEntityMapper contextMapper, final HsCredentialsEntity entity) {
        this.contextMapper = contextMapper;
        this.entity = entity;
    }

    @Override
    public void apply(final CredentialsPatchResource resource) {
        if ( resource.getActive() != null ) {
                entity.setActive(resource.getActive());
        }
        OptionalFromJson.of(resource.getEmailAddress())
                .ifPresent(entity::setEmailAddress);
        OptionalFromJson.of(resource.getTotpSecret())
                .ifPresent(entity::setTotpSecret);
        OptionalFromJson.of(resource.getSmsNumber())
                .ifPresent(entity::setSmsNumber);
        OptionalFromJson.of(resource.getPhonePassword())
                .ifPresent(entity::setPhonePassword);
        if (resource.getContexts() != null) {
            contextMapper.syncCredentialsContextEntities(resource.getContexts(), entity.getLoginContexts());
        }
    }

}
