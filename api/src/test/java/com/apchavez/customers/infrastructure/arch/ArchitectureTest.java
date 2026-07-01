package com.apchavez.customers.infrastructure.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.apchavez.customers",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("com.apchavez.customers.domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.apchavez.customers.infrastructure..")
                    .as("El dominio no debe depender de la infraestructura");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application =
            noClasses()
                    .that().resideInAPackage("com.apchavez.customers.domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.apchavez.customers.application..")
                    .as("El dominio no debe depender de la capa de aplicación");

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("com.apchavez.customers.application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.apchavez.customers.infrastructure..")
                    .as("La capa de aplicación no debe depender de la infraestructura");

    @ArchTest
    static final ArchRule web_layer_should_not_call_domain_service_directly =
            noClasses()
                    .that().resideInAPackage("com.apchavez.customers.infrastructure.web..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.apchavez.customers.domain.service..")
                    .as("La capa web debe acceder al dominio solo a través del servicio de aplicación");
}
