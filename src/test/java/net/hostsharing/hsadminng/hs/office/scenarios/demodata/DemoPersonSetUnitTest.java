package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoPersonSetUnitTest {

    @TempDir
    Path tempDir;

    @Test
    void readsTheFormatProducedByTheAccountTooling() throws Exception {
        val csv = write("""
                "subjectName","subjectUuid","givenName","familyName","personUuid"
                "hsh00-admin","","Sabine","Kraus",""
                "hsh00-board","","Torsten","Vogt",""
                """);

        val fixedPersons = DemoPersonSet.load(csv);

        assertThat(fixedPersons).hasSize(2);
        assertThat(fixedPersons.get(0).subjectName()).isEqualTo("hsh00-admin");
        assertThat(fixedPersons.get(0).givenName()).isEqualTo("Sabine");
        assertThat(fixedPersons.get(0).familyName()).isEqualTo("Kraus");
        assertThat(DemoPersonSet.fullNamesOf(fixedPersons)).containsExactlyInAnyOrder("Sabine Kraus", "Torsten Vogt");
    }

    @Test
    void readsTheHeaderProducedByTheKeycloakExport() throws Exception {
        val csv = write("""
                "username","uuid","firstName","lastName"
                "abc-sabine","3fa85f64-5717-4562-b3fc-2c963f66afa6","Sabine","Kraus"
                """);

        assertThat(DemoPersonSet.load(csv)).singleElement()
                .extracting(DemoPersonSet.FixedPerson::subjectUuid)
                .isEqualTo("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    }

    @Test
    void groupsLoginsOfOneCustomerByTheirPrefix() throws Exception {
        val customers = DemoPersonSet.customersOf(DemoPersonSet.load(write("""
                "abc-sabine","","Sabine","Kraus",""
                "vogtcorp-torsten","","Torsten","Vogt",""
                "vogtcorp-hanna","","Hanna","Vogt",""
                """)));

        assertThat(customers).hasSize(2);
        assertThat(customers.get(0).prefix()).isEqualTo("abc");
        assertThat(customers.get(0).isNaturalPerson()).isTrue();
        assertThat(customers.get(0).defaultPrefix()).isEqualTo("abc");
        assertThat(customers.get(1).persons()).hasSize(2);
        assertThat(customers.get(1).isNaturalPerson()).isFalse();
        assertThat(customers.get(1).defaultPrefix()).isNull();
        assertThat(customers.get(1).tradeNameCore()).isEqualTo("Vogtcorp");
    }

    @Test
    void staffLoginsGetNoCustomerOfTheirOwn() throws Exception {
        val customers = DemoPersonSet.customersOf(DemoPersonSet.load(write("""
                "hostsharing-sabine","","Sabine","Kraus",""
                "abc-sabine","","Sabine","Kraus",""
                """)));

        assertThat(customers).singleElement().extracting(DemoPersonSet.FixedCustomer::prefix).isEqualTo("abc");
    }

    @Test
    void writesThePersonUuidOfBothLoginsOfOneHuman() throws Exception {
        val fixedPersons = DemoPersonSet.load(write("""
                "hostsharing-sabine","s-uuid","Sabine","Kraus"
                "abc-sabine","a-uuid","Sabine M.","Kraus"
                "xyz-torsten","x-uuid","Torsten","Vogt"
                """));
        val target = tempDir.resolve("out.csv");

        DemoPersonSet.writeWithPersonUuids(target, fixedPersons, Map.of("kraus|sabine", "person-uuid"));

        assertThat(Files.readAllLines(target)).containsExactly(
                "\"subjectName\",\"subjectUuid\",\"givenName\",\"familyName\",\"personUuid\"",
                "\"hostsharing-sabine\",\"s-uuid\",\"Sabine\",\"Kraus\",\"person-uuid\"",
                "\"abc-sabine\",\"a-uuid\",\"Sabine M.\",\"Kraus\",\"person-uuid\"",
                "\"xyz-torsten\",\"x-uuid\",\"Torsten\",\"Vogt\",\"\"");
    }

    @Test
    void derivesTheOutputPathFromTheInputPath() {
        assertThat(DemoPersonSet.outputPathFor(Path.of(".private/keycloak-users.csv")))
                .isEqualTo(Path.of(".private/keycloak-users-with-person-uuids.csv"));
    }

    @Test
    void acceptsUnquotedValuesAndAlreadyResolvedUuids() throws Exception {
        val csv = write("""
                subjectName,subjectUuid,givenName,familyName,personUuid
                hsh00-admin,3fa85f64-5717-4562-b3fc-2c963f66afa6,Sabine,Kraus,7c9e6679-7425-40de-944b-e07fc1f90ae7
                """);

        assertThat(DemoPersonSet.load(csv)).singleElement()
                .extracting(DemoPersonSet.FixedPerson::familyName).isEqualTo("Kraus");
    }

    @Test
    void skipsEmptyAndCommentLines() throws Exception {
        val csv = write("""
                # exported via tools/get-keycloak-users
                "subjectName","subjectUuid","givenName","familyName","personUuid"

                "hsh00-admin","","Sabine","Kraus",""
                """);

        assertThat(DemoPersonSet.load(csv)).hasSize(1);
    }

    @Test
    void rejectsLinesWithTooFewColumns() throws Exception {
        val csv = write("""
                "hsh00-admin","","Sabine"
                """);

        assertThatThrownBy(() -> DemoPersonSet.load(csv))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subjectName, subjectUuid, givenName, familyName, personUuid");
    }

    @Test
    void yieldsNoFixedPersonsWithoutTheProperty() {
        assertThat(System.getProperty(DemoPersonSet.CSV_PATH_PROPERTY)).isNull();

        assertThat(DemoPersonSet.configured()).isEmpty();
    }

    private Path write(final String content) throws Exception {
        val csv = tempDir.resolve("persons.csv");
        Files.writeString(csv, content);
        return csv;
    }
}
