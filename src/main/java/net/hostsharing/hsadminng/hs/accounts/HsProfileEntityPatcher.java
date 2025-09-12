package net.hostsharing.hsadminng.hs.accounts;

import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ProfilePatchResource;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import net.hostsharing.hsadminng.mapper.OptionalFromJson;

import java.util.Optional;

public class HsProfileEntityPatcher implements EntityPatcher<ProfilePatchResource> {

    private ScopeResourceToEntityMapper scopeMapper;
    private final HsProfileEntity entity;

    public HsProfileEntityPatcher(final ScopeResourceToEntityMapper scopeMapper, final HsProfileEntity entity) {
        this.scopeMapper = scopeMapper;
        this.entity = entity;
    }

    @Override
    public void apply(final ProfilePatchResource resource) {
        if ( resource.getActive() != null ) {
                entity.setActive(resource.getActive());
        }
        OptionalFromJson.of(resource.getEmailAddress())
                .ifPresent(entity::setEmailAddress);
        Optional.ofNullable(resource.getTotpSecrets())
                .ifPresent(entity::setTotpSecrets);
        OptionalFromJson.of(resource.getSmsNumber())
                .ifPresent(entity::setSmsNumber);
        OptionalFromJson.of(resource.getPhonePassword())
                .ifPresent(entity::setPhonePassword);
        if (resource.getScopes() != null) {
            scopeMapper.syncProfileScopeEntities(resource.getScopes(), entity.getScopes());
        }
    }

}
