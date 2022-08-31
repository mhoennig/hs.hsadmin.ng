package net.hostsharing.hsadminng.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.lang.ArchRule;
import net.hostsharing.hsadminng.Accepts;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = ArchTest.NET_HOSTSHARING_HSADMINNG)
public class ArchTest {

    public static final String NET_HOSTSHARING_HSADMINNG = "net.hostsharing.hsadminng";

    @com.tngtech.archunit.junit.ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule contextPackageRule = classes()
            .that().resideInAPackage("..context..")
            .should().onlyDependOnClassesThat()
            .resideOutsideOfPackage(NET_HOSTSHARING_HSADMINNG);

    @com.tngtech.archunit.junit.ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule configPackageRule = classes()
            .that().resideInAPackage("..config..")
            .should().onlyDependOnClassesThat()
            .resideOutsideOfPackage(NET_HOSTSHARING_HSADMINNG);

    @com.tngtech.archunit.junit.ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule errorsPackageRule = classes()
            .that().resideInAPackage("..errors..")
            .should().onlyDependOnClassesThat()
            .resideOutsideOfPackage(NET_HOSTSHARING_HSADMINNG);

    @com.tngtech.archunit.junit.ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsPackagesRule = classes()
            .that().resideInAPackage("..test.(*)..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage("..test.(*)..");

    @com.tngtech.archunit.junit.ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsPackagePackageRule = classes()
            .that().resideInAPackage("..test.pac..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage("..test.pac..");

    @com.tngtech.archunit.junit.ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule acceptsAnnotationOnMethodsRule = methods()
            .that().areAnnotatedWith(Accepts.class)
            .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("AcceptanceTest")
            .orShould().beDeclaredInClassesThat().haveSimpleNameNotContaining("AcceptanceTest$");

    @com.tngtech.archunit.junit.ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule acceptsAnnotationOnClasesRule = classes()
            .that().areAnnotatedWith(Accepts.class)
            .should().haveSimpleNameEndingWith("AcceptanceTest")
            .orShould().haveSimpleNameNotContaining("AcceptanceTest$");

    @com.tngtech.archunit.junit.ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule doNotUseJavaxTransactionAnnotationAtClassLevel = noClasses()
            .should().beAnnotatedWith(javax.transaction.Transactional.class.getName())
            .as("Use @%s instead of @%s.".formatted(
                    org.springframework.transaction.annotation.Transactional.class.getName(),
                    javax.transaction.Transactional.class));

    @com.tngtech.archunit.junit.ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule doNotUseJavaxTransactionAnnotationAtMethodLevel = noMethods()
            .should().beAnnotatedWith(javax.transaction.Transactional.class)
            .as("Use @%s instead of @%s.".formatted(
                    org.springframework.transaction.annotation.Transactional.class.getName(),
                    javax.transaction.Transactional.class.getName()));

    @com.tngtech.archunit.junit.ArchTest
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
