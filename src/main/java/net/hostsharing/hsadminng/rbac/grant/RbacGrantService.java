package net.hostsharing.hsadminng.rbac.grant;

import net.hostsharing.hsadminng.rbac.role.RbacRoleEntity;
import net.hostsharing.hsadminng.rbac.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RbacGrantService {

    @Autowired
    private RbacGrantRepository rbacGrantRepo;

    public class RbacRoleGranter{
        private final RbacRoleEntity role;

        public RbacRoleGranter(final RbacRoleEntity role) {
            this.role = role;
        }

        public void to(final Subject subject) {
            rbacGrantRepo.save(RbacGrantEntity.builder()
                    .grantedRoleUuid(role.getUuid())
                    .granteeSubjectUuid(subject.getUuid())
                    .assumed(true)
                    .build());
        }
    }

    public RbacRoleGranter grant(final RbacRoleEntity role) {
        return new RbacRoleGranter(role);
    }

}
