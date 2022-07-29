-- ========================================================
-- Customer example with RBAC
-- --------------------------------------------------------

set session session authorization default;

create table if not exists customer
(
    uuid          uuid unique references RbacObject (uuid),
    reference     int not null unique check (reference between 10000 and 99999),
    prefix        character(3) unique,
    adminUserName varchar(63)
);

drop trigger if exists createRbacObjectForCustomer_Trigger on customer;
create trigger createRbacObjectForCustomer_Trigger
    before insert
    on customer
    for each row
execute procedure createRbacObject();

create or replace function customerOwner(customer customer)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('customer', customer.uuid, 'owner');
end; $$;

create or replace function customerAdmin(customer customer)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('customer', customer.uuid, 'admin');
end; $$;

create or replace function customerTenant(customer customer)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('customer', customer.uuid, 'tenant');
end; $$;


create or replace function createRbacRulesForCustomer()
    returns trigger
    language plpgsql
    strict as $$
declare
    customerOwnerUuid uuid;
    customerAdminUuid uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    -- the owner role with full access for Hostsharing administrators
    customerOwnerUuid = createRole(
        customerOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(hostsharingAdmin())
        );

    -- the admin role for the customer's admins, who can view and add products
    customerAdminUuid = createRole(
        customerAdmin(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view', 'add-package']),
        -- NO auto follow for customer owner to avoid exploding permissions for administrators
        withUser(NEW.adminUserName, 'create') -- implicitly ignored if null
        );

    -- allow the customer owner role (thus administrators) to assume the customer admin role
    call grantRoleToRole(customerAdminUuid, customerOwnerUuid, false);

    -- the tenant role which later can be used by owners+admins of sub-objects
    perform createRole(
        customerTenant(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view'])
        );

    return NEW;
end; $$;

drop trigger if exists createRbacRulesForCustomer_Trigger on customer;
create trigger createRbacRulesForCustomer_Trigger
    after insert
    on customer
    for each row
execute procedure createRbacRulesForCustomer();

create or replace function deleteRbacRulesForCustomer()
    returns trigger
    language plpgsql
    strict as $$
declare
    objectTable varchar = 'customer';
begin
    if TG_OP = 'DELETE' then

        -- delete the owner role (for admininstrators)
        call deleteRole(findRoleId(objectTable || '#' || NEW.prefix || '.owner'));

        -- delete the customer admin role
        call deleteRole(findRoleId(objectTable || '#' || NEW.prefix || '.admin'));
    else
        raise exception 'invalid usage of TRIGGER BEFORE DELETE';
    end if;
end; $$;

drop trigger if exists deleteRbacRulesForCustomer_Trigger on customer;
create trigger deleteRbacRulesForCustomer_Trigger
    before delete
    on customer
    for each row
execute procedure deleteRbacRulesForCustomer();

-- create a restricted view to access the textual customer ids a idName
set session session authorization default;
-- ALTER TABLE customer ENABLE ROW LEVEL SECURITY;
drop view if exists customer_iv;
create or replace view customer_iv as
select distinct target.uuid, target.prefix as idName
    from customer as target;
-- TODO: Is it ok that everybody has access to this information?
grant all privileges on customer_iv to restricted;

create or replace function customerUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from customer_iv iv where iv.idName = customerUuidByIdName.idName;
$$;

-- create RBAC restricted view
set session session authorization default;
-- ALTER TABLE customer ENABLE ROW LEVEL SECURITY;
drop view if exists customer_rv;
create or replace view customer_rv as
select distinct target.*
    from customer as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'customer', currentSubjectIds()));
grant all privileges on customer_rv to restricted;


-- generate Customer test data

set session session authorization default;
do language plpgsql $$
    declare
        currentTask   varchar;
        custReference integer;
        custRowId     uuid;
        custPrefix    varchar;
        custAdminName varchar;
    begin
        set hsadminng.currentUser to '';

        for t in 0..9
            loop
                currentTask = 'creating RBAC test customer #' || t;
                set local hsadminng.currentUser to 'mike@hostsharing.net';
                set local hsadminng.assumedRoles = '';
                set local hsadminng.currentTask to currentTask;

                -- When a new customer is created,
                custReference = 10000 + t;
                custRowId = uuid_generate_v4();
                custPrefix = intToVarChar(t, 3);
                custAdminName = 'admin@' || custPrefix || '.example.com';

                raise notice 'creating customer %:%', custReference, custPrefix;
                insert
                    into customer (reference, prefix, adminUserName)
                    values (custReference, custPrefix, custAdminName);

                commit;

            end loop;

    end;
$$;
