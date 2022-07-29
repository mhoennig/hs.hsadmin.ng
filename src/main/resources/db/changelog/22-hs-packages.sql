-- ========================================================
-- Package example with RBAC
-- --------------------------------------------------------

set session session authorization default;

create table if not exists package
(
    uuid         uuid unique references RbacObject (uuid),
    name         character varying(5),
    customerUuid uuid references customer (uuid)
);

create or replace function packageOwner(pac package)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
declare
    roleDesc RbacRoleDescriptor;
begin
    return roleDescriptor('package', pac.uuid, 'admin');
end; $$;

create or replace function packageAdmin(pac package)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('package', pac.uuid, 'admin');
end; $$;

create or replace function packageTenant(pac package)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('package', pac.uuid, 'tenant');
end; $$;


drop trigger if exists createRbacObjectForPackage_Trigger on package;
create trigger createRbacObjectForPackage_Trigger
    before insert
    on package
    for each row
execute procedure createRbacObject();

create or replace function createRbacRulesForPackage()
    returns trigger
    language plpgsql
    strict as $$
declare
    parentCustomer       customer;
    packageOwnerRoleUuid uuid;
    packageAdminRoleUuid uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    select * from customer as c where c.uuid = NEW.customerUuid into parentCustomer;

    -- an owner role is created and assigned to the customer's admin role
    packageOwnerRoleUuid = createRole(
        packageOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(customerAdmin(parentCustomer))
        );

    -- an owner role is created and assigned to the package owner role
    packageAdminRoleUuid = createRole(
        packageAdmin(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['edit', 'add-unixuser', 'add-domain']),
        beneathRole(packageOwnerRoleUuid)
        );

    -- and a package tenant role is created and assigned to the package admin as well
    perform createRole(
        packageTenant(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view']),
        beneathRole(packageAdminRoleUuid),
        beingItselfA(customerTenant(parentCustomer))
        );

    return NEW;
end; $$;

drop trigger if exists createRbacRulesForPackage_Trigger on package;
create trigger createRbacRulesForPackage_Trigger
    after insert
    on package
    for each row
execute procedure createRbacRulesForPackage();

create or replace function deleteRbacRulesForPackage()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP = 'DELETE' then
        --  TODO
    else
        raise exception 'invalid usage of TRIGGER BEFORE DELETE';
    end if;
end; $$;

drop trigger if exists deleteRbacRulesForPackage_Trigger on customer;
create trigger deleteRbacRulesForPackage_Trigger
    before delete
    on customer
    for each row
execute procedure deleteRbacRulesForPackage();

-- create RBAC-restricted view
set session session authorization default;
-- ALTER TABLE package ENABLE ROW LEVEL SECURITY;
drop view if exists package_rv;
create or replace view package_rv as
select distinct target.*
    from package as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'package', currentSubjectIds()));
grant all privileges on package_rv to restricted;


-- generate Package test data

do language plpgsql $$
    declare
        cust        customer;
        pacName     varchar;
        currentTask varchar;
        custAdmin   varchar;
    begin
        set hsadminng.currentUser to '';

        for cust in (select * from customer)
            loop
                -- CONTINUE WHEN cust.reference < 18000;

                for t in 0..randominrange(1, 2)
                    loop
                        pacName = cust.prefix || to_char(t, 'fm00');
                        currentTask = 'creating RBAC test package #' || pacName || ' for customer ' || cust.prefix || ' #' ||
                                      cust.uuid;
                        raise notice 'task: %', currentTask;

                        custAdmin = 'admin@' || cust.prefix || '.example.com';
                        set local hsadminng.currentUser to custAdmin;
                        set local hsadminng.assumedRoles = '';
                        set local hsadminng.currentTask to currentTask;

                        insert
                            into package (name, customerUuid)
                            values (pacName, cust.uuid);

                        commit;
                    end loop;
            end loop;
    end;
$$;

