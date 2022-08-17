package net.hostsharing.hsadminng.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import net.hostsharing.hsadminng.Accepts;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = ArchUnitTest.NET_HOSTSHARING_HSADMINNG)
public class ArchUnitTest {

    public static final String NET_HOSTSHARING_HSADMINNG = "net.hostsharing.hsadminng";

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
    public static final ArchRule hsPackagesRule = classes()
        .that().resideInAPackage("..hs.(*)..")
        .should().onlyBeAccessed().byClassesThat()
        .resideInAnyPackage("..hs.(*)..");

    @ArchTest
    @SuppressWarnings("unused")
    public static final ArchRule hsPackagePackageRule = classes()
        .that().resideInAPackage("..hs.hspackage..")
        .should().onlyBeAccessed().byClassesThat()
        .resideInAnyPackage("..hs.hspackage..");

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
