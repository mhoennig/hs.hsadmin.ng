-- ========================================================
-- Some Tests
-- --------------------------------------------------------


select isGranted(findRoleId('administrators'), findRoleId('test_package#aaa00.owner'));
select isGranted(findRoleId('test_package#aaa00.owner'), findRoleId('administrators'));
-- call grantRoleToRole(findRoleId('test_package#aaa00.owner'), findRoleId('administrators'));
-- call grantRoleToRole(findRoleId('administrators'), findRoleId('test_package#aaa00.owner'));

select count(*)
FROM queryAllPermissionsOfSubjectIdForObjectUuids(findRbacUser('superuser-fran@hostsharing.net'),
                                                  ARRAY(select uuid from customer where reference < 1100000));
select count(*)
FROM queryAllPermissionsOfSubjectId(findRbacUser('superuser-fran@hostsharing.net'));
select *
FROM queryAllPermissionsOfSubjectId(findRbacUser('alex@example.com'));
select *
FROM queryAllPermissionsOfSubjectId(findRbacUser('rosa@example.com'));

select *
FROM queryAllRbacUsersWithPermissionsFor(findEffectivePermissionId('customer',
                                                          (SELECT uuid FROM RbacObject WHERE objectTable = 'customer' LIMIT 1),
                                                          'add-package'));
select *
FROM queryAllRbacUsersWithPermissionsFor(findEffectivePermissionId('package',
                                                          (SELECT uuid FROM RbacObject WHERE objectTable = 'package' LIMIT 1),
                                                          'delete'));

DO LANGUAGE plpgsql
$$
    DECLARE
        userId uuid;
        result bool;
    BEGIN
        userId = findRbacUser('superuser-alex@hostsharing.net');
        result = (SELECT * FROM isPermissionGrantedToSubject(findEffectivePermissionId('package', 94928, 'add-package'), userId));
        IF (result) THEN
            RAISE EXCEPTION 'expected permission NOT to be granted, but it is';
        end if;

        result = (SELECT * FROM isPermissionGrantedToSubject(findEffectivePermissionId('package', 94928, 'view'), userId));
        IF (NOT result) THEN
            RAISE EXCEPTION 'expected permission to be granted, but it is NOT';
        end if;

        RAISE LOG 'isPermissionGrantedToSubjectId test passed';
    END;
$$;

