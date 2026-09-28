package com.empresa.asiscontrol.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.empresa.asiscontrol", importOptions = ImportOption.DoNotIncludeTests.class)
class ModularArchitectureTest {

    @ArchTest
    static final ArchRule controllersStayInsideTheirDomain = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule servicesStayInsideTheirDomain = classes()
            .that().haveSimpleNameEndingWith("Service")
            .should().resideInAnyPackage("..service..", "..security..");

    @ArchTest
    static final ArchRule controllersDoNotAccessPersistence = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..", "..entity..");

    @ArchTest
    static final ArchRule repositoriesAreDomainLocal = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule phase2ModulesUsePublicServicesAcrossDomains = noClasses()
            .that().resideInAnyPackage("..areas..", "..cargos..", "..empleados..",
                    "..supervisores..", "..cuadrillas..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..usuarios.repository..", "..roles.repository..", "..auth.repository..",
                    "..auditoria.repository..");

    @ArchTest
    static final ArchRule employeeModuleDoesNotReadOtherPhase2Repositories = noClasses()
            .that().resideInAPackage("..empleados..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..areas.repository..", "..cargos.repository..",
                    "..supervisores.repository..", "..cuadrillas.repository..");

    @ArchTest
    static final ArchRule crewAndSupervisorModulesDoNotReadEmployeeRepository = noClasses()
            .that().resideInAnyPackage("..supervisores..", "..cuadrillas..")
            .should().dependOnClassesThat().resideInAPackage("..empleados.repository..");
}
