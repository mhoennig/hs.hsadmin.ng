package net.hostsharing.hsadminng.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.test.Accepts;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = ArchitectureTest.NET_HOSTSHARING_HSADMINNG)
public class ArchitectureTest {

    public static final String NET_HOSTSHARING_HSADMINNG = "net.hostsharing.hsadminng";

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule onlyValidPackages = noClasses()
            .that().doNotBelongToAnyOf(HsadminNgApplication.class, ArchitectureTest.class)
            .should().resideOutsideOfPackages(
                    // ATTENTION: Don't simply add packages here, also add arch rules for the new package!
                    "..config",
                    "..test",
                    "..test.cust",
                    "..test.pac",
                    "..test.dom",
                    "..context",
                    "..generated..",
                    "..persistence..",
                    "..hs.office.bankaccount",
                    "..hs.office.contact",
                    "..hs.office.coopassets",
                    "..hs.office.coopshares",
                    "..hs.office.debitor",
                    "..hs.office.membership",
                    "..hs.office.migration",
                    "..hs.office.partner",
                    "..hs.office.person",
                    "..hs.office.relation",
                    "..hs.office.sepamandate",
                    "..errors",
                    "..mapper",
                    "..ping",
                    "..rbac",
                    "..rbac.rbacuser",
                    "..rbac.rbacgrant",
                    "..rbac.rbacrole",
                    "..rbac.rbacobject",
                    "..rbac.rbacdef",
                    "..stringify"
                    // ATTENTION: Don't simply add packages here, also add arch rules for the new package!
            );

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule doNotUseJUnit4 = noClasses()
            .should().accessClassesThat()
            .resideInAPackage("org.junit");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule dontUseImplSuffix = noClasses()
            .should().haveSimpleNameEndingWith("Impl");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule testClassesAreProperlyNamed = classes()
            .that().haveSimpleNameEndingWith("Test")
            .and().doNotHaveModifier(ABSTRACT)
            .should().haveNameMatching(".*(UnitTest|RestTest|IntegrationTest|AcceptanceTest|ArchitectureTest)$");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule allPackagesBelowNetHostsharingHsAdmin = noClasses()
            .should().resideOutsideOfPackages(NET_HOSTSHARING_HSADMINNG + "..");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule contextPackageRule = classes()
            .that().resideInAPackage("..context..")
            .should().onlyDependOnClassesThat()
            .resideOutsideOfPackage(NET_HOSTSHARING_HSADMINNG);

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule configPackageRule = classes()
            .that().resideInAPackage("..config..")
            .should().onlyDependOnClassesThat()
            .resideOutsideOfPackage(NET_HOSTSHARING_HSADMINNG);

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule errorsPackageRule = classes()
            .that().resideInAPackage("..errors..")
            .should().onlyDependOnClassesThat()
            .resideOutsideOfPackage(NET_HOSTSHARING_HSADMINNG);

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule testPackagesRule = classes()
            .that().resideInAPackage("..test.(*)..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage("..test.(*)..");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule testPackagePackageRule = classes()
            .that().resideInAPackage("..test.pac..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage("..test.pac..");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsAdminPackagesRule = classes()
            .that().resideInAPackage("..hs.office.(*)..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(
                    "..hs.office.(*)..",
                    "..rbac.rbacgrant" // TODO: just because of RbacGrantsDiagramServiceIntegrationTest
            );

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsOfficeBankAccountPackageRule = classes()
            .that().resideInAPackage("..hs.office.bankaccount..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(
                    "..hs.office.bankaccount..",
                    "..hs.office.sepamandate..",
                    "..hs.office.debitor..",
                    "..hs.office.migration..");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsOfficeSepaMandatePackageRule = classes()
            .that().resideInAPackage("..hs.office.sepamandate..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(
                    "..hs.office.sepamandate..",
                    "..hs.office.debitor..",
                    "..hs.office.migration..");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsOfficeContactPackageRule = classes()
            .that().resideInAPackage("..hs.office.contact..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(
                    "..hs.office.contact..",
                    "..hs.office.relation..",
                    "..hs.office.partner..",
                    "..hs.office.debitor..",
                    "..hs.office.membership..",
                    "..hs.office.migration..");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsOfficePersonPackageRule = classes()
            .that().resideInAPackage("..hs.office.person..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(
                    "..hs.office.person..",
                    "..hs.office.relation..",
                    "..hs.office.partner..",
                    "..hs.office.debitor..",
                    "..hs.office.membership..",
                    "..hs.office.migration..")
            .orShould().haveNameNotMatching(".*Test$");


    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsOfficeRelationPackageRule = classes()
            .that().resideInAPackage("..hs.office.relation..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(
                    "..hs.office.relation..",
                    "..hs.office.partner..",
                    "..hs.office.migration..")
            .orShould().haveNameNotMatching(".*Test$");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsOfficePartnerPackageRule = classes()
            .that().resideInAPackage("..hs.office.partner..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(
                    "..hs.office.partner..",
                    "..hs.office.debitor..",
                    "..hs.office.membership..",
                    "..hs.office.migration..")
            .orShould().haveNameNotMatching(".*Test$");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsOfficeMembershipPackageRule = classes()
            .that().resideInAPackage("..hs.office.membership..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(
                    "..hs.office.membership..",
                    "..hs.office.coopassets..",
                    "..hs.office.coopshares..",
                    "..hs.office.migration..");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsOfficeCoopAssetsPackageRule = classes()
        .that().resideInAPackage("..hs.office.coopassets..")
        .should().onlyBeAccessed().byClassesThat()
        .resideInAnyPackage(
                "..hs.office.coopassets..",
                "..hs.office.migration..");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsOfficeCoopSharesPackageRule = classes()
            .that().resideInAPackage("..hs.office.coopshares..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(
                    "..hs.office.coopshares..",
                    "..hs.office.migration..");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsOfficeMigrationPackageRule = classes()
            .that().resideInAPackage("..hs.office.migration..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage("..hs.office.migration..");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule acceptsAnnotationOnMethodsRule = methods()
            .that().areAnnotatedWith(Accepts.class)
            .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("AcceptanceTest")
            .orShould().beDeclaredInClassesThat().haveSimpleNameNotContaining("AcceptanceTest$");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule acceptsAnnotationOnClasesRule = classes()
            .that().areAnnotatedWith(Accepts.class)
            .should().haveSimpleNameEndingWith("AcceptanceTest")
            .orShould().haveSimpleNameNotContaining("AcceptanceTest$");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule doNotUseJakartaTransactionAnnotationAtClassLevel = noClasses()
            .should().beAnnotatedWith(jakarta.transaction.Transactional.class.getName())
            .as("Use @%s instead of @%s.".formatted(
                    org.springframework.transaction.annotation.Transactional.class.getName(),
                    jakarta.transaction.Transactional.class));

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule doNotUseJakartaTransactionAnnotationAtMethodLevel = noMethods()
            .should().beAnnotatedWith(jakarta.transaction.Transactional.class)
            .as("Use @%s instead of @%s.".formatted(
                    org.springframework.transaction.annotation.Transactional.class.getName(),
                    jakarta.transaction.Transactional.class.getName()));

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule doNotUseOrgJUnitTestAnnotation = noMethods()
            .should().beAnnotatedWith(org.junit.Test.class)
            .as("Use @%s instead of @%s.".formatted(
                    org.junit.jupiter.api.Test.class.getName(),
                    org.junit.Test.class.getName()));

    @Test
    public void everythingShouldBeFreeOfCycles() {
        slices().matching("net.hostsharing.hsadminng.(*)..").should().beFreeOfCycles();
    }

    @Test
    public void restControllerNaming() {
        classes().that().areAnnotatedWith(RestController.class).should().haveSimpleNameEndingWith("Controller");
    }

    @Test
    public void repositoryNaming() {
        classes().that().implement(JpaRepository.class).should().haveSimpleNameEndingWith("Repository");
    }
}
