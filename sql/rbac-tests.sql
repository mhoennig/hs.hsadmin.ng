-- ========================================================
-- Some Tests
-- --------------------------------------------------------


select rbac.isGranted(rbac.findRoleId('administrators'), rbac.findRoleId('test.package#aaa00:OWNER'));
select rbac.isGranted(rbac.findRoleId('test.package#aaa00:OWNER'), rbac.findRoleId('administrators'));
-- call rbac.grantRoleToRole(findRoleId('test.package#aaa00:OWNER'), findRoleId('administrators'));
-- call rbac.grantRoleToRole(findRoleId('administrators'), findRoleId('test.package#aaa00:OWNER'));

select count(*)
FROM rbac.queryAllPermissionsOfSubjectIdForObjectUuids(rbac.findRbacSubject('superuser-fran@hostsharing.net'),
                                                  ARRAY(select uuid from test.customer where reference < 1100000));
select count(*)
FROM rbac.queryAllPermissionsOfSubjectId(findRbacSubject('superuser-fran@hostsharing.net'));
select *
FROM rbac.queryAllPermissionsOfSubjectId(findRbacSubject('alex@example.com'));
select *
FROM rbac.queryAllPermissionsOfSubjectId(findRbacSubject('rosa@example.com'));

select *
FROM rbac.queryAllRbacSubjectsWithPermissionsFor(rbac.findEffectivePermissionId('customer',
                                                          (SELECT uuid FROM rbac.RbacObject WHERE objectTable = 'customer' LIMIT 1),
                                                          'add-package'));
select *
FROM rbac.queryAllRbacSubjectsWithPermissionsFor(rbac.findEffectivePermissionId('package',
                                                          (SELECT uuid FROM rbac.RbacObject WHERE objectTable = 'package' LIMIT 1),
                                                          'DELETE'));

DO LANGUAGE plpgsql
$$
    DECLARE
        userId uuid;
        result bool;
    BEGIN
        userId = rbac.findRbacSubject('superuser-alex@hostsharing.net');
        result = (SELECT * FROM rbac.isPermissionGrantedToSubject(rbac.findPermissionId('package', 94928, 'add-package'), userId));
        IF (result) THEN
            RAISE EXCEPTION 'expected permission NOT to be granted, but it is';
        end if;

        result = (SELECT * FROM rbac.isPermissionGrantedToSubject(rbac.findPermissionId('package', 94928, 'SELECT'), userId));
        IF (NOT result) THEN
            RAISE EXCEPTION 'expected permission to be granted, but it is NOT';
        end if;

        RAISE LOG 'isPermissionGrantedToSubjectId test passed';
    END;
$$;

