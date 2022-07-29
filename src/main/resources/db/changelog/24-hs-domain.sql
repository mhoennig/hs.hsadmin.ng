-- ========================================================
-- Domain example with RBAC
-- --------------------------------------------------------

set session session authorization default;

create table if not exists Domain
(
    uuid         uuid unique references RbacObject (uuid),
    name         character varying(32),
    unixUserUuid uuid references unixuser (uuid)
);

drop trigger if exists createRbacObjectForDomain_Trigger on Domain;
create trigger createRbacObjectForDomain_Trigger
    before insert
    on Domain
    for each row
execute procedure createRbacObject();

create or replace function domainOwner(dom Domain)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('domain', dom.uuid, 'owner');
end; $$;

create or replace function domainAdmin(dom Domain)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('domain', dom.uuid, 'admin');
end; $$;

create or replace function domainTenant(dom Domain)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('domain', dom.uuid, 'tenant');
end; $$;


create or replace function createRbacRulesForDomain()
    returns trigger
    language plpgsql
    strict as $$
declare
    parentUser          UnixUser;
    parentPackage       package;
    domainOwnerRoleUuid uuid;
    domainAdminRoleUuid uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    select * from UnixUser where uuid = NEW.unixUserUuid into parentUser;
    select * from Package where uuid = parentUser.packageuuid into parentPackage;

    -- a domain owner role is created and assigned to the unixuser's admin role
    domainOwnerRoleUuid = createRole(
        domainOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(packageAdmin(parentPackage))
        );

    -- a domain admin role is created and assigned to the domain's owner role
    domainAdminRoleUuid = createRole(
        domainAdmin(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['edit', 'add-emailaddress']),
        beneathRole(domainOwnerRoleUuid)
        );

    -- and a domain tenant role is created and assigned to the domain's admiin role
    perform createRole(
        domainTenant(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(domainAdminRoleUuid),
        beingItselfA(createUnixUserTenantRoleIfNotExists(parentUser))
        );

    return NEW;
end; $$;

drop trigger if exists createRbacRulesForDomain_Trigger on Domain;
create trigger createRbacRulesForDomain_Trigger
    after insert
    on Domain
    for each row
execute procedure createRbacRulesForDomain();

-- TODO: CREATE OR REPLACE FUNCTION deleteRbacRulesForDomain()


-- create RBAC-restricted view
set session session authorization default;
-- ALTER TABLE Domain ENABLE ROW LEVEL SECURITY;
drop view if exists domain_rv;
create or replace view domain_rv as
select distinct target.*
    from Domain as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'domain', currentSubjectIds()));
grant all privileges on domain_rv to restricted;


-- generate Domain test data

do language plpgsql $$
    declare
        uu          record;
        pac         package;
        pacAdmin    varchar;
        currentTask varchar;
    begin
        set hsadminng.currentUser to '';

        for uu in (select u.uuid, u.name, u.packageuuid, c.reference
                       from unixuser u
                                join package p on u.packageuuid = p.uuid
                                join customer c on p.customeruuid = c.uuid
            -- WHERE c.reference >= 18000
        )
            loop
                if (random() < 0.3) then
                    for t in 0..1
                        loop
                            currentTask = 'creating RBAC test Domain #' || t || ' for UnixUser ' || uu.name || ' #' || uu.uuid;
                            raise notice 'task: %', currentTask;

                            select * from package where uuid = uu.packageUuid into pac;
                            pacAdmin = 'admin@' || pac.name || '.example.com';
                            set local hsadminng.currentUser to pacAdmin;
                            set local hsadminng.assumedRoles = '';
                            set local hsadminng.currentTask to currentTask;

                            insert
                                into Domain (name, unixUserUuid)
                                values ('dom-' || t || '.' || uu.name || '.example.org', uu.uuid);

                            commit;
                        end loop;
                end if;
            end loop;

    end;
$$;


