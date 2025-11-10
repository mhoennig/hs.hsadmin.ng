package net.hostsharing.hsadminng.hs.accounts.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.scenarios.Produces;
import net.hostsharing.hsadminng.hs.scenarios.Requires;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.mapper.Array;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;

import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.as;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;

class ProfileScenarioTests extends ScenarioTest {

  @SneakyThrows
  @BeforeEach
  protected void beforeScenario(final TestInfo testInfo) {
    super.beforeScenario(testInfo);
  }

  @Nested
  @Order(10)
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class RbacContextScenarios {

    @Test
    @Order(1010)
    @Produces("RBAC Context")
    void shouldFetchRbacContext() {
      new FetchRbacContext(scenarioTest)
          .given("subjectName", "superuser-fran@hostsharing.net")
          .given("assumedRoles", "rbactest.package#xxx00:ADMIN;rbactest.package#yyy00:ADMIN")
          .given("expectedToBeGlobalAdmin", true)
          .thenExpect(HttpStatus.OK)
          .keep();
    }
  }

  @Nested
  @Order(20)
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class CurrentLoginUserScenarios {

    @Test
    @Order(2010)
    @Produces("Current Login User")
    void shouldFetchCurrentLoginUser() {
      new CurrentLoginUser(scenarioTest)
          .given("subjectName", "superuser-fran@hostsharing.net")
          .given("personGivenName", "Fran")
          .given("expectedToBeGlobalAdmin", true)
          .thenExpect(HttpStatus.OK)
          .keep();
    }
  }

  @Nested
  @Order(30)
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class ProfileScenarios {

    @Test
    @Order(1010)
    @Produces(
        explicitly = "Profile: susan-firby",
        implicitly = {"Person: Susan Firby"})
    void shouldCreateInitialProfileForExistingNaturalPerson() {
      new CreateProfileForExistingPerson(scenarioTest, asGlobalAgent())
          // to find a specific existing person
          .given("personFamilyName", "Firby")
          .given("personGivenName", "Susan")
          // a login name, to be stored in the new RBAC subject
          .given("nickname", "firby-susan")
          // initial profile
          .given("emailAddress", "susan.firby@example.com")
          .given("smsNumber", "+49123456789")
          .given("password", "my raw password")
          .given("totpSecrets", Array.of("initialSecret"))
          .given("phonePassword", "securePass123")
          .given("globalUid", 21011)
          .given("globalGid", 21011)
          .given("active", true)
                    .given(
                            "scopes", Array.of(
                                    Pair.of("HSADMIN", "prod")
                            ))
          .thenExpect(HttpStatus.OK)
          .keep();
    }

    @Test
    @Order(1020)
    @Requires("Profile: susan-firby")
    void naturalPersonShouldBeAbleToUpdateTheirOwnProfile() {
      new UpdateProfile(scenarioTest, as("firby-susan"))
          // the profile to update
          .given("profileUuid", "%{Profile: susan-firby}")
          // updated profile
          .given("active", false)
          .given("totpSecrets", Array.of("initialSecret", "additionalSecret"))
          .given("emailAddress", "susan.firby@example.org")
          .given("password", "my new raw password")
          .given("phonePassword", "securePass987")
          .given("smsNumber", "+49987654321")
          .given("scopes", Array.of(Pair.of("HSADMIN", "prod"), Pair.of("SSH", "external")))
          .thenExpect(HttpStatus.OK);
    }

    @Test
    @Order(1100)
    @Produces(
        explicitly = "Profile: peter-newman",
        implicitly = {"Person: Peter Newman"})
    void shouldCreateInitialProfileForNewNaturalPerson() {
      new CreateProfileForNewPerson(scenarioTest, asGlobalAgent())
          // to find a specific existing person
          .given("personFamilyName", "Newman")
          .given("personGivenName", "Peter")
          // a login name, to be stored in the new RBAC subject
          .given("nickname", "newman-peter")
          // initial profile
          .given("emailAddress", "peter.newman@example.com")
          .given("smsNumber", "+49123456789")
          .given("password", "my raw password")
          .given("totpSecrets", Array.of("initialSecret"))
          .given("phonePassword", "securePass123")
          .given("globalUid", 21012)
          .given("globalGid", 21012)
          .given("active", true)
          .given("scopes", Array.of(Pair.of("HSADMIN", "prod")))
          .thenExpect(HttpStatus.OK)
          .keep();
    }

    @Test
    @Order(1110)
    @Requires("Profile: peter-newman")
    void newNaturalPersonShouldBeAbleToUpdateTheirOwnProfile() {
      new UpdateProfile(scenarioTest, as("newman-peter"))
          // the profile to update
          .given("profileUuid", "%{Profile: peter-newman}")
          // updated profile
          .given("active", false)
          .given("totpSecrets", Array.of("initialSecret", "additionalSecret"))
          .given("emailAddress", "peter.newman@example.org")
          .given("password", "my new raw password")
          .given("phonePassword", "securePass987")
          .given("smsNumber", "+49987654321")
                    .given(
                            "scopes", Array.of(
                                    Pair.of("HSADMIN", "prod"),
                                    Pair.of("SSH", "external")
                            ))
          .thenExpect(HttpStatus.OK);
    }

    @Test
    @Order(1120)
    @Requires({"Profile: peter-newman", "Profile: susan-firby"})
    // Usually, scenario tests just test positive cases, but in the case of account profiles, security is extra important,
    // thus I've added also some negative cases like this one for documentation reasons.
    // More negative cases are tested in "so-called" Acceptance and in RestTests.
    void anotherNaturalPersonShouldNotBeAbleToUpdateOthersProfile() {
      new UpdateProfile(scenarioTest, as("firby-susan"))
              // the profile to update
              .given("profileUuid", "%{Profile: peter-newman}")
              // updated profile
              .given("active", false)
              .given("totpSecrets", Array.of("initialSecret", "additionalSecret"))
              .given("emailAddress", "peter.newman@example.org")
              .given("password", "my new raw password")
              .given("phonePassword", "securePass987")
              .given("smsNumber", "+49987654321")
              .given("scopes", Array.of(Pair.of("HSADMIN", "prod"), Pair.of("SSH", "external")))
              .thenExpect(HttpStatus.FORBIDDEN);
    }
  }
}
