--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:rbac-subject-RENAME-TEST-DATA-SUBJECTS context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Renames all remaining test-data subjects from the old email-address format to
    the new realm-prefixed format.  The seed subjects were already renamed in
    1080-rbac-global.sql; this changeset covers the subjects created by the
    2-rbactest and 5-hs-office test-data generators.

    The UPDATE statements are idempotent: rows that already carry a valid new name
    are not matched by the WHERE clause and are left unchanged.
 */
do language plpgsql $$
    begin
        call base.defineContext('renaming test-data subjects to realm-prefixed format', null, null, null);
        -- rbactest customer admin subjects: customer-admin@{prefix}.example.com -> tst-customer_admin_{prefix}
        update rbac.subject
            set name = 'tst-customer_admin_' || lower(regexp_replace(name, '^customer-admin@(.+)\.example\.com$', '\1'))
            where name ~ '^customer-admin@[a-z]+\.example\.com$';

        -- rbactest package admin subjects: pac-admin-{pacName}@{prefix}.example.com -> tst-pac_admin_{pacName}
        update rbac.subject
            set name = 'tst-pac_admin_' || lower(regexp_replace(name, '^pac-admin-([^@]+)@[^@]+$', '\1'))
            where name ~ '^pac-admin-[a-z0-9]+@[a-z]+\.example\.com$';

        -- contact admin subjects: contact-admin@{contactId}.example.com -> tst-contact_admin_{contactId}
        update rbac.subject
            set name = 'tst-contact_admin_' || lower(regexp_replace(name, '^contact-admin@(.+)\.example\.com$', '\1'))
            where name ~ '^contact-admin@[a-z]+\.example\.com$';

        -- person subjects: person-{cleanIdentifier}@example.com -> tst-person_{lower(replace(id, '-', '_'))}
        update rbac.subject
            set name = 'tst-person_' || lower(replace(regexp_replace(name, '^person-(.+)@example\.com$', '\1'), '-', '_'))
            where name ~ '^person-[A-Za-z0-9_-]+@example\.com$';

        -- bankaccount admin subjects: bankaccount-admin@{cleanIdentifier}.example.com -> tst-bankaccount_{lower(...)}
        update rbac.subject
            set name = 'tst-bankaccount_' || lower(replace(regexp_replace(name, '^bankaccount-admin@(.+)\.example\.com$', '\1'), '-', '_'))
            where name ~ '^bankaccount-admin@[A-Za-z0-9_-]+\.example\.com$';
    end;
$$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-subject-CHECK-SUBJECT-NAME-CHECK endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Enforces the naming conventions for subjects.
    In non-test environments (context:without-test-data), the subjects have to be renamed before this migration runs.
    If not, the ALTER TABLE is going to fail.

    Uses separate constraint names so PostgreSQL validation errors can be mapped
    to type-specific translated messages.
 */
alter table rbac.subject
    drop constraint if exists check_valid_user_subject_name,
    add constraint check_valid_user_subject_name
    check (
        type <> 'USER'::rbac.SubjectType or rbac.is_valid_user_subject_name(name)
    ),
    add constraint check_valid_group_subject_name
    check (
        type <> 'GROUP'::rbac.SubjectType or rbac.is_valid_group_subject_name(name)
    );
--//


--changeset michael.hoennig:rbac-subject-DROP-SUBJECT-NAME-CHECK endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    With the separate organization column (1050-rbac-base.sql), subject names no longer need
    to carry a derivable realm-prefix, thus the naming patterns are no longer enforced at the
    DB level. The pattern functions are dropped as well; they still have to be created in
    1050-rbac-base.sql because the (already released) constraint changeset above depends on them.
 */
alter table rbac.subject
    drop constraint if exists check_valid_user_subject_name,
    drop constraint if exists check_valid_group_subject_name;
drop function if exists rbac.is_valid_user_subject_name(varchar);
drop function if exists rbac.is_valid_group_subject_name(varchar);
--//


--changeset michael.hoennig:rbac-subject-BACKFILL-ORGANIZATION endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Back-fills the organization column added in 1050-rbac-base.sql from the subject-name prefix
    for pre-existing subjects, after all old-format names got renamed by the changesets above.
    New subjects get their organization explicitly or via the default trigger, so the column
    can be `not null` from here on.
 */
do language plpgsql $$
    begin
        call base.defineContext('back-filling subject organization from the name prefix', null, null, null);
        update rbac.subject
            set organization = rbac.subject_realm_prefix(name)
            where organization is null;
    end;
$$;
alter table rbac.subject
    alter column organization set not null;
--//
