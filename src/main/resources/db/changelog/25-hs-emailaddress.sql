-- ========================================================
-- EMailAddress example with RBAC
-- --------------------------------------------------------

set session session authorization default;

create table if not exists EMailAddress
(
    uuid       uuid unique references RbacObject (uuid),
    localPart  character varying(64),
    domainUuid uuid references domain (uuid)
);

drop trigger if exists createRbacObjectForEMailAddress_Trigger on EMailAddress;
create trigger createRbacObjectForEMailAddress_Trigger
    before insert
    on EMailAddress
    for each row
execute procedure createRbacObject();

create or replace function emailAddressOwner(emAddr EMailAddress)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('emailaddress', emAddr.uuid, 'owner');
end; $$;

create or replace function emailAddressAdmin(emAddr EMailAddress)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('emailaddress', emAddr.uuid, 'admin');
end; $$;

create or replace function createRbacRulesForEMailAddress()
    returns trigger
    language plpgsql
    strict as $$
declare
    parentDomain              Domain;
    eMailAddressOwnerRoleUuid uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    select d.*
        from domain d
                 left join unixuser u on u.uuid = d.unixuseruuid
        where d.uuid = NEW.domainUuid
        into parentDomain;

    -- an owner role is created and assigned to the domains's admin group
    eMailAddressOwnerRoleUuid = createRole(
        emailAddressOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(domainAdmin(parentDomain))
        );

    -- and an admin role is created and assigned to the unixuser owner as well
    perform createRole(
        emailAddressAdmin(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['edit']),
        beneathRole(eMailAddressOwnerRoleUuid),
        beingItselfA(domainTenant(parentDomain))
        );

    return NEW;
end; $$;

drop trigger if exists createRbacRulesForEMailAddress_Trigger on EMailAddress;
create trigger createRbacRulesForEMailAddress_Trigger
    after insert
    on EMailAddress
    for each row
execute procedure createRbacRulesForEMailAddress();

-- TODO: CREATE OR REPLACE FUNCTION deleteRbacRulesForEMailAddress()


-- create RBAC-restricted view
set session session authorization default;
-- ALTER TABLE EMailAddress ENABLE ROW LEVEL SECURITY;
drop view if exists EMailAddress_rv;
create or replace view EMailAddress_rv as
select target.*
    from EMailAddress as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'emailaddress', currentSubjectIds()));
grant all privileges on EMailAddress_rv to restricted;

-- generate EMailAddress test data

do language plpgsql $$
    declare
        dom         record;
        pacAdmin    varchar;
        currentTask varchar;
    begin
        set hsadminng.currentUser to '';

        for dom in (select d.uuid, d.name, p.name as packageName
                        from domain d
                                 join unixuser u on u.uuid = d.unixuseruuid
                                 join package p on u.packageuuid = p.uuid
                                 join customer c on p.customeruuid = c.uuid
            -- WHERE c.reference >= 18000
        )
            loop
                for t in 0..4
                    loop
                        currentTask = 'creating RBAC test EMailAddress #' || t || ' for Domain ' || dom.name;
                        raise notice 'task: %', currentTask;

                        pacAdmin = 'admin@' || dom.packageName || '.example.com';
                        set local hsadminng.currentUser to pacAdmin;
                        set local hsadminng.assumedRoles = '';
                        set local hsadminng.currentTask to currentTask;

                        insert
                            into EMailAddress (localPart, domainUuid)
                            values ('local' || t, dom.uuid);

                        commit;
                    end loop;
            end loop;
    end;
$$;


