// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MembershipDTOTest {

    @Test
    public void withShouldApplyCallback() {
        final MembershipDTO actual = new MembershipDTO().with(m -> m.setRemark("Some Remark"));

        assertThat(actual.getRemark()).isEqualTo("Some Remark");
    }
}
