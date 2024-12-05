package net.hostsharing.hsadminng.hs.office.person;

import net.hostsharing.hsadminng.rbac.generator.RbacViewMermaidFlowchartGenerator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficePersonEntityUnitTest {

    @Test
    void getDisplayReturnsTradeNameIfAvailable() {
        final var givenPersonEntity = HsOfficePersonRbacEntity.builder()
                .personType(HsOfficePersonType.LEGAL_PERSON)
                .tradeName("some trade name")
                .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("LP some trade name");
    }

    @Test
    void getDisplayReturnsFamilyAndGivenNameIfNoTradeNameAvailable() {
        final var givenPersonEntity = HsOfficePersonRbacEntity.builder()
                .personType(HsOfficePersonType.NATURAL_PERSON)
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("NP some family name, some given name");
    }

    @Test
    void toShortStringWithTradeNameReturnsTradeName() {
        final var givenPersonEntity = HsOfficePersonRbacEntity.builder()
                .personType(HsOfficePersonType.LEGAL_PERSON)
                .tradeName("some trade name")
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("LP some trade name");
    }

    @Test
    void toShortStringWithoutTradeNameReturnsFamilyAndGivenName() {
        final var givenPersonEntity = HsOfficePersonRbacEntity.builder()
                .personType(HsOfficePersonType.NATURAL_PERSON)
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("NP some family name, some given name");
    }

    @Test
    void toShortStringWithSalutationAndTitleReturnsSalutationAndTitle() {
        final var givenPersonEntity = HsOfficePersonRbacEntity.builder()
            .personType(HsOfficePersonType.NATURAL_PERSON)
            .salutation("Frau")
            .title("Dr.")
            .familyName("some family name")
            .givenName("some given name")
            .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("NP some family name, some given name");
    }

    @Test
    void toShortStringWithSalutationAndWithoutTitleReturnsSalutation() {
        final var givenPersonEntity = HsOfficePersonRbacEntity.builder()
            .personType(HsOfficePersonType.NATURAL_PERSON)
            .salutation("Frau")
            .familyName("some family name")
            .givenName("some given name")
            .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("NP some family name, some given name");
    }

    @Test
    void toShortStringWithoutSalutationAndWithTitleReturnsTitle() {
        final var givenPersonEntity = HsOfficePersonRbacEntity.builder()
            .personType(HsOfficePersonType.NATURAL_PERSON)
            .title("Dr. Dr.")
            .familyName("some family name")
            .givenName("some given name")
            .build();

        final var actualDisplay = givenPersonEntity.toShortString();

        assertThat(actualDisplay).isEqualTo("NP some family name, some given name");
    }

    @Test
    void toStringWithAllFieldsReturnsAllButUuid() {
        final var givenPersonEntity = HsOfficePersonRbacEntity.builder()
                .uuid(UUID.randomUUID())
                .personType(HsOfficePersonType.NATURAL_PERSON)
                .tradeName("some trade name")
                .title("Dr.")
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toString();

        assertThat(actualDisplay).isEqualTo("person(personType='NP', tradeName='some trade name', title='Dr.', familyName='some family name', givenName='some given name')");
    }

    @Test
    void toStringSkipsNullFields() {
        final var givenPersonEntity = HsOfficePersonRbacEntity.builder()
                .familyName("some family name")
                .givenName("some given name")
                .build();

        final var actualDisplay = givenPersonEntity.toString();

        assertThat(actualDisplay).isEqualTo("person(familyName='some family name', givenName='some given name')");
    }
    @Test
    void toStringWithSalutationAndTitleRetursSalutationAndTitle() {
        final var givenPersonEntity = HsOfficePersonRbacEntity.builder()
            .salutation("Herr")
            .title("Prof. Dr.")
            .familyName("some family name")
            .givenName("some given name")
            .build();

        final var actualDisplay = givenPersonEntity.toString();

        assertThat(actualDisplay).isEqualTo("person(salutation='Herr', title='Prof. Dr.', familyName='some family name', givenName='some given name')");
    }
    @Test
    void toStringWithSalutationAndWithoutTitleSkipsTitle() {
        final var givenPersonEntity = HsOfficePersonRbacEntity.builder()
            .salutation("Herr")
            .familyName("some family name")
            .givenName("some given name")
            .build();

        final var actualDisplay = givenPersonEntity.toString();

        assertThat(actualDisplay).isEqualTo("person(salutation='Herr', familyName='some family name', givenName='some given name')");
    }

    @Test
    void toStringWithoutSalutationAndWithTitleSkipsSalutation() {
        final var givenPersonEntity = HsOfficePersonRbacEntity.builder()
            .title("some title")
            .familyName("some family name")
            .givenName("some given name")
            .build();

        final var actualDisplay = givenPersonEntity.toString();

        assertThat(actualDisplay).isEqualTo("person(title='some title', familyName='some family name', givenName='some given name')");
    }

    @Test
    void definesRbac() {
        final var rbacFlowchart = new RbacViewMermaidFlowchartGenerator(HsOfficePersonRbacEntity.rbac()).toString();
        assertThat(rbacFlowchart).isEqualTo("""
                %%{init:{'flowchart':{'htmlLabels':false}}}%%
                flowchart TB
                
                subgraph person["`**person**`"]
                    direction TB
                    style person fill:#dd4901,stroke:#274d6e,stroke-width:8px
                
                    subgraph person:roles[ ]
                        style person:roles fill:#dd4901,stroke:white
                
                        role:person:OWNER[[person:OWNER]]
                        role:person:ADMIN[[person:ADMIN]]
                        role:person:REFERRER[[person:REFERRER]]
                    end
                
                    subgraph person:permissions[ ]
                        style person:permissions fill:#dd4901,stroke:white
                
                        perm:person:INSERT{{person:INSERT}}
                        perm:person:DELETE{{person:DELETE}}
                        perm:person:UPDATE{{person:UPDATE}}
                        perm:person:SELECT{{person:SELECT}}
                    end
                end
                
                %% granting roles to users
                user:creator ==> role:person:OWNER
                
                %% granting roles to roles
                role:rbac.global:ADMIN ==> role:person:OWNER
                role:person:OWNER ==> role:person:ADMIN
                role:person:ADMIN ==> role:person:REFERRER
                
                %% granting permissions to roles
                role:rbac.global:GUEST ==> perm:person:INSERT
                role:person:OWNER ==> perm:person:DELETE
                role:person:ADMIN ==> perm:person:UPDATE
                role:person:REFERRER ==> perm:person:SELECT
                """);
    }
}
