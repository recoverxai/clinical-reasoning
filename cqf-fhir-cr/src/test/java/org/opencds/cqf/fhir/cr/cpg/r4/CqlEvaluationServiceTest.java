package org.opencds.cqf.fhir.cr.cpg.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.r4.Parameters.datePart;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

class CqlEvaluationServiceTest {
    @Test
    void libraryEvaluationService_inlineAsthma() {
        var content =
                """
        library AsthmaTest version '1.0.0'

        using FHIR version '4.0.1'

        include FHIRHelpers version '4.0.1'

        codesystem "SNOMED": 'http://snomed.info/sct'

        code "Asthma": '195967001' from "SNOMED"

        context Patient

        define "Asthma Diagnosis"

        [Condition: "Asthma"]
            define "Has Asthma Diagnosis":
                exists("Asthma Diagnosis")
        """;
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .subject("Patient/SimplePatient")
                .content(content)
                .evaluateCql();
        var results = when.then().parameters();
        assertTrue(results.hasParameter());
        assertEquals(3, results.getParameter().size());
    }

    @Test
    void libraryEvaluationService_contentAndExpression() {
        var content =
                """
        library SimpleR4Library

        using FHIR version '4.0.1'

        include FHIRHelpers version '4.0.1' called FHIRHelpers

        context Patient

        define simpleBooleanExpression: true

        define observationRetrieve: [Observation]

        define observationHasCode: not IsNull(([Observation]).code)

        define "Initial Population": observationHasCode

        define "Denominator": "Initial Population"

        define "Numerator": "Denominator"";
        """;
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .subject("Patient/SimplePatient")
                .expression("Numerator")
                .content(content)
                .evaluateCql();
        var results = when.then().parameters();
        assertFalse(results.isEmpty());
        assertEquals(1, results.getParameter().size());
        assertInstanceOf(BooleanType.class, results.getParameter("Numerator").getValue());
        assertTrue(((BooleanType) results.getParameter("Numerator").getValue()).booleanValue());
    }

    @Test
    void libraryEvaluationService_arithmetic() {
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .expression("5*5")
                .evaluateCql();
        var results = when.then().parameters();
        assertInstanceOf(IntegerType.class, results.getParameter("return").getValue());
        assertEquals("25", ((IntegerType) results.getParameter("return").getValue()).asStringValue());
    }

    @Test
    void libraryEvaluationService_paramsAndExpression() {
        Parameters evaluationParams = parameters(datePart("%inputDate", "2019-11-01"));
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .subject("Patient/SimplePatient")
                .parameters(evaluationParams)
                .expression("year from %inputDate before 2020")
                .evaluateCql();
        var results = when.then().parameters();
        assertInstanceOf(BooleanType.class, results.getParameter("return").getValue());
        assertTrue(((BooleanType) results.getParameter("return").getValue()).booleanValue());
    }

    @Test
    void libraryEvaluationService_ErrorLibrary() {
        var expression = "Interval[1,5]";
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .expression(expression)
                .evaluateCql();
        var report = when.then().parameters();
        assertTrue(report.hasParameter());
        assertTrue(report.getParameterFirstRep().hasName());
        assertEquals("evaluation error", report.getParameterFirstRep().getName());
        assertTrue(report.getParameterFirstRep().hasResource());
        assertInstanceOf(OperationOutcome.class, report.getParameterFirstRep().getResource());
        assertEquals(
                "Unsupported interval point type for FHIR conversion java.lang.Integer",
                ((OperationOutcome) report.getParameterFirstRep().getResource())
                        .getIssueFirstRep()
                        .getDetails()
                        .getText());
    }
}
