package net.hostsharing.hsadminng.rbac.generator;

import net.hostsharing.hsadminng.rbac.generator.RbacSpec.RbacGrantDefinition;

import java.util.HashSet;
import java.util.Set;
import static net.hostsharing.hsadminng.rbac.generator.StringWriter.with;

class RbacRbacSystemRebuildGenerator {

    private final RbacSpec rbacDef;
    private final Set<RbacGrantDefinition> rbacGrants = new HashSet<>();
    private final String liquibaseTagPrefix;
    private final String rawTableName;

    RbacRbacSystemRebuildGenerator(final RbacSpec rbacDef, final String liquibaseTagPrefix) {
        this.rbacDef = rbacDef;
        this.liquibaseTagPrefix = liquibaseTagPrefix;
        this.rawTableName = rbacDef.getRootEntityAlias().getRawTableNameWithSchema();
    }

    void generateTo(final StringWriter plPgSql) {
        plPgSql.writeLn("""
                -- ============================================================================
                --changeset RbacRbacSystemRebuildGenerator:${liquibaseTagPrefix}-rbac-rebuild endDelimiter:--//
                -- ----------------------------------------------------------------------------

                -- HOWTO: Rebuild RBAC-system for table ${rawTableName} after changing its RBAC specification.
                --
                -- begin transaction;
                --  call base.defineContext('re-creating RBAC for table ${rawTableName}', null, <<insert executing global admin user here>>);
                --  call ${rawTableName}_rebuild_rbac_system();
                -- commit;
                --
                -- How it works:
                -- 1. All grants previously created from the RBAC specification of this table will be deleted.
                --    These grants are identified by `${rawTableName}.grantedByTriggerOf IS NOT NULL`.
                --    User-induced grants (`${rawTableName}.grantedByTriggerOf IS NULL`) are NOT deleted.
                -- 2. New role types will be created, but existing role types which are not specified anymore,
                --    will NOT be deleted!
                -- 3. All newly specified grants will be created.
                --
                -- IMPORTANT:
                -- Make sure not to skip any previously defined role-types or you might break indirect grants!
                -- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
                -- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
                -- of this table would be in a dead end.
    
                create or replace procedure ${rawTableName}_rebuild_rbac_system()
                    language plpgsql as $$
                DECLARE
                    DECLARE
                    row ${rawTableName};
                    grantsAfter numeric;
                    grantsBefore numeric;
                BEGIN
                    SELECT count(*) INTO grantsBefore FROM rbac.grant;
    
                    FOR row IN SELECT * FROM ${rawTableName} LOOP
                            -- first delete all generated grants for this row from the previously defined RBAC system
                            DELETE FROM rbac.grant g
                                   WHERE g.grantedbytriggerof = row.uuid;
    
                            -- then build the grants according to the currently defined RBAC rules
                            CALL ${rawTableName}_build_rbac_system(row);
                        END LOOP;
    
                    select count(*) into grantsAfter from rbac.grant;
    
                    -- print how the total count of grants has changed
                    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
                END;
                $$;
                --//

                """,
                with("liquibaseTagPrefix", liquibaseTagPrefix),
                with("rawTableName", rawTableName));
    }
}
