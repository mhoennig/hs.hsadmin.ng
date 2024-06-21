package net.hostsharing.hsadminng.arch;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.rbacgrant.RbacGrantsDiagramService;
import net.hostsharing.hsadminng.rbac.rbacobject.RbacObject;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.Table;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static java.lang.String.format;

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
                    "..validation..",
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
                    "..hs.booking.debitor",
                    "..hs.booking.project",
                    "..hs.booking.item",
                    "..hs.booking.item.validators",
                    "..hs.hosting.asset",
                    "..hs.hosting.asset.validators",
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
    public static final ArchRule hsOfficePackageAccessRule = classes()
            .that().resideInAPackage("..hs.office.(*)..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(
                    "..hs.office.(*)..",
                    "..hs.booking.(*)..",
                    "..hs.hosting.(*)..",
                    "..rbac.rbacgrant" // TODO.test: just because of RbacGrantsDiagramServiceIntegrationTest
            );

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsBookingPackageAccessRule = classes()
            .that().resideInAPackage("..hs.booking.(*)..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(
                    "..hs.booking.(*)..",
                    "..hs.hosting.(*)..",
                    "..hs.validation" // TODO.impl: Some Validators need to be refactored to booking package.
            );

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsHostingPackageAccessRule = classes()
            .that().resideInAPackage("..hs.hosting.(*)..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(
                    "..hs.hosting.(*)..",
                    "..hs.booking.(*).." // TODO.impl: fix this cyclic dependency
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
                    "..hs.office.migration..",
                    "..hs.hosting.asset.."
                    );

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

    @ArchTest
    @SuppressWarnings("unused")
    static final ArchRule everythingShouldBeFreeOfCycles =
        slices().matching("net.hostsharing.hsadminng.(*)..")
                .should().beFreeOfCycles()
                // TODO.refa: would be great if we could get rid of these cyclic dependencies
                .ignoreDependency(
                        ContextBasedTest.class,
                        RbacGrantsDiagramService.class)
                .ignoreDependency(
                        HsBookingItemEntity.class,
                        HsHostingAssetEntity.class);


    @ArchTest
    @SuppressWarnings("unused")
    static final ArchRule restControllerNaming =
        classes().that().areAnnotatedWith(RestController.class).should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    @SuppressWarnings("unused")
    static final ArchRule repositoryNaming =
        classes().that().areAssignableTo(Repository.class).should().haveSimpleNameEndingWith("Repository");

    @ArchTest
    @SuppressWarnings("unused")
    static final ArchRule tableNamesOfRbacEntitiesShouldEndWith_rv =
        classes()
                .that().areAnnotatedWith(Table.class)
                .and().areAssignableTo(RbacObject.class)
                .should(haveTableNameEndingWith_rv())
                .because("it's required that the table names of RBAC entities end with '_rv'");

    static ArchCondition<JavaClass> haveTableNameEndingWith_rv() {
        return new ArchCondition<>("RBAC table name end with _rv") {

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                final var table = javaClass.getAnnotationOfType(Table.class);
                if (table == null) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            format("@Table annotation missing for RBAC entity %s",
                                javaClass.getName(), table.name())));
                } else if (!table.name().endsWith("_rv")) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            format("Table name of %s does not end with '_rv' for RBAC entity %s",
                                javaClass.getName(), table.name())));
                }
            }
        };
    }
}
