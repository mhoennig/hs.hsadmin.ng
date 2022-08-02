-- ========================================================
-- UnixUser example with RBAC
-- --------------------------------------------------------

set session session authorization default;

create table if not exists UnixUser
(
    uuid        uuid unique references RbacObject (uuid),
    name        character varying(32),
    comment     character varying(96),
    packageUuid uuid references package (uuid)
);

create or replace function unixUserOwner(uu UnixUser)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('unixuser', uu.uuid, 'owner');
end; $$;

create or replace function unixUserAdmin(uu UnixUser)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('unixuser', uu.uuid, 'admin');
end; $$;

create or replace function unixUserTenant(uu UnixUser)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('unixuser', uu.uuid, 'tenant');
end; $$;

create or replace function createUnixUserTenantRoleIfNotExists(unixUser UnixUser)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    unixUserTenantRoleDesc RbacRoleDescriptor;
    unixUserTenantRoleUuid uuid;
begin
    unixUserTenantRoleDesc = unixUserTenant(unixUser);
    unixUserTenantRoleUuid = findRoleId(unixUserTenantRoleDesc);
    if unixUserTenantRoleUuid is not null then
        return unixUserTenantRoleUuid;
    end if;

    return createRole(
        unixUserTenantRoleDesc,
        grantingPermissions(forObjectUuid => unixUser.uuid, permitOps => array ['view']),
        beneathRole(unixUserAdmin(unixUser))
        );
end; $$;


drop trigger if exists createRbacObjectForUnixUser_Trigger on UnixUser;
create trigger createRbacObjectForUnixUser_Trigger
    before insert
    on UnixUser
    for each row
execute procedure createRbacObject();

create or replace function createRbacRulesForUnixUser()
    returns trigger
    language plpgsql
    strict as $$
declare
    parentPackage       package;
    unixuserOwnerRoleId uuid;
    unixuserAdminRoleId uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    select * from package where uuid = NEW.packageUuid into parentPackage;

    -- an owner role is created and assigned to the package's admin group
    unixuserOwnerRoleId = createRole(
        unixUserOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(packageAdmin(parentPackage))
        );

    -- and a unixuser admin role is created and assigned to the unixuser owner as well
    unixuserAdminRoleId = createRole(
        unixUserAdmin(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['edit']),
        beneathRole(unixuserOwnerRoleId),
        beingItselfA(packageTenant(parentPackage))
        );

    -- a tenent role is only created on demand

    return NEW;
end; $$;

drop trigger if exists createRbacRulesForUnixUser_Trigger on UnixUser;
create trigger createRbacRulesForUnixUser_Trigger
    after insert
    on UnixUser
    for each row
execute procedure createRbacRulesForUnixUser();

-- TODO: CREATE OR REPLACE FUNCTION deleteRbacRulesForUnixUser()


-- create RBAC-restricted view
set session session authorization default;
-- ALTER TABLE unixuser ENABLE ROW LEVEL SECURITY;
drop view if exists unixuser_rv;
create or replace view unixuser_rv as
select target.*
    from unixuser as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'unixuser', currentSubjectIds()));
grant all privileges on unixuser_rv to restricted;


-- generate UnixUser test data

do language plpgsql $$
    declare
        pac         record;
        pacAdmin    varchar;
        currentTask varchar;
    begin
        set hsadminng.currentUser to '';

        for pac in (select p.uuid, p.name
                        from package p
                                 join customer c on p.customeruuid = c.uuid
            -- WHERE c.reference >= 18000
        )
            loop

                for t in 0..9
                    loop
                        currentTask = 'creating RBAC test unixuser #' || t || ' for package ' || pac.name || ' #' || pac.uuid;
                        raise notice 'task: %', currentTask;
                        pacAdmin = 'admin@' || pac.name || '.example.com';
                        set local hsadminng.currentUser to 'mike@hostsharing.net'; -- TODO: use a package-admin
                        set local hsadminng.assumedRoles = '';
                        set local hsadminng.currentTask to currentTask;

                        insert
                            into unixuser (name, packageUuid)
                            values (pac.name || '-' || intToVarChar(t, 4), pac.uuid);

                        commit;
                    end loop;
            end loop;

    end;
$$;
