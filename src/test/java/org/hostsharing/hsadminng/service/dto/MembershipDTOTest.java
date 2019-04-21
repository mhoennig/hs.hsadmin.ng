package org.hostsharing.hsadminng.service.dto;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MembershipDTOTest {

    @Test
    public void withShouldApplyCallback() {
        final MembershipDTO actual = new MembershipDTO().with(m -> m.setRemark("Some Remark"));

        assertThat(actual.getRemark()).isEqualTo("Some Remark");
    }
}
