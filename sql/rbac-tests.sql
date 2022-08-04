-- ========================================================
-- Some Tests
-- --------------------------------------------------------


select isGranted(findRoleId('administrators'), findRoleId('package#aaa00.owner'));
select isGranted(findRoleId('package#aaa00.owner'), findRoleId('administrators'));
-- call grantRoleToRole(findRoleId('package#aaa00.owner'), findRoleId('administrators'));
-- call grantRoleToRole(findRoleId('administrators'), findRoleId('package#aaa00.owner'));

select count(*)
FROM queryAllPermissionsOfSubjectIdForObjectUuids(findRbacUser('sven@hostsharing.net'),
                                                  ARRAY(select uuid from customer where reference < 1100000));
select count(*)
FROM queryAllPermissionsOfSubjectId(findRbacUser('sven@hostsharing.net'));
select *
FROM queryAllPermissionsOfSubjectId(findRbacUser('alex@example.com'));
select *
FROM queryAllPermissionsOfSubjectId(findRbacUser('rosa@example.com'));

select *
FROM queryAllRbacUsersWithPermissionsFor(findPermissionId('customer',
                                                          (SELECT uuid FROM RbacObject WHERE objectTable = 'customer' LIMIT 1),
                                                          'add-package'));
select *
FROM queryAllRbacUsersWithPermissionsFor(findPermissionId('package',
                                                          (SELECT uuid FROM RbacObject WHERE objectTable = 'package' LIMIT 1),
                                                          'delete'));

DO LANGUAGE plpgsql
$$
    DECLARE
        userId uuid;
        result bool;
    BEGIN
        userId = findRbacUser('mike@hostsharing.net');
        result = (SELECT * FROM isPermissionGrantedToSubject(findPermissionId('package', 94928, 'add-package'), userId));
        IF (result) THEN
            RAISE EXCEPTION 'expected permission NOT to be granted, but it is';
        end if;

        result = (SELECT * FROM isPermissionGrantedToSubject(findPermissionId('package', 94928, 'view'), userId));
        IF (NOT result) THEN
            RAISE EXCEPTION 'expected permission to be granted, but it is NOT';
        end if;

        RAISE LOG 'isPermissionGrantedToSubjectId test passed';
    END;
$$;

