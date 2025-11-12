package com.yunke.backend.document.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocRoleMapperTest {

    private final DocRoleMapper mapper = new DocRoleMapper();

    @Test
    void mapsCommenterRoleToMaskAndBack() {
        int mask = mapper.roleToMask("commenter");
        assertThat(mask).isNotZero();
        assertThat(mapper.maskToRole(mask)).isEqualTo("commenter");
    }

    @Test
    void resolvesMaskFromExplicitPermission() {
        int mask = 123;
        assertThat(mapper.resolveMask(null, mask)).isEqualTo(mask);
    }

    @Test
    void fallsBackToReaderWhenMaskOnlyHasReadBit() {
        int mask = mapper.roleToMask("reader");
        assertThat(mapper.maskToRole(mask)).isEqualTo("reader");
    }
}
