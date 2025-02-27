package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.PATIENT_ID;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newPopulateRequestForVersion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;

@ExtendWith(MockitoExtension.class)
class PopulateProcessorTests {
    @Mock
    private Repository repository;

    @Mock
    private LibraryEngine libraryEngine;

    @Spy
    private final PopulateProcessor fixture = new PopulateProcessor();

    @BeforeEach
    void setup() {
        doReturn(repository).when(libraryEngine).getRepository();
    }

    @Test
    void populateShouldReturnQuestionnaireResponseResourceWithPopulatedFieldsR4() {
        // setup
        final String questionnaireUrl = "original-questionnaire-url";
        final String prePopulatedQuestionnaireId = "prepopulated-questionnaire-id";
        final var originalQuestionnaire = new Questionnaire();
        originalQuestionnaire.setId(prePopulatedQuestionnaireId);
        originalQuestionnaire.setUrl(questionnaireUrl);
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final PopulateRequest request =
                newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, originalQuestionnaire);
        final var expectedResponses = getExpectedResponses(request);
        doReturn(expectedResponses).when(fixture).processItems(request, Collections.emptyList());
        // execute
        final IBaseResource actual = fixture.populate(request);
        // validate
        assertEquals(
                prePopulatedQuestionnaireId + "-" + PATIENT_ID,
                actual.getIdElement().getIdPart());
        assertContainedOperationOutcome(request, actual, null);
        assertEquals(questionnaireUrl, request.resolvePathString(actual, "questionnaire"));
        assertEquals(
                QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS,
                ((QuestionnaireResponse) actual).getStatus());
        assertEquals(
                "Patient/" + PATIENT_ID,
                request.resolvePath(actual, "subject", IBaseReference.class)
                        .getReferenceElement()
                        .getValue());
        assertEquals(expectedResponses, request.getItems(actual));
        verify(fixture).processItems(request, Collections.emptyList());
    }

    @Test
    void populateShouldReturnQuestionnaireResponseResourceWithPopulatedFieldsR5() {
        // setup
        final String questionnaireUrl = "original-questionnaire-url";
        final String prePopulatedQuestionnaireId = "prepopulated-questionnaire-id";
        final var originalQuestionnaire = new org.hl7.fhir.r5.model.Questionnaire();
        originalQuestionnaire.setId(prePopulatedQuestionnaireId);
        originalQuestionnaire.setUrl(questionnaireUrl);
        doReturn(FhirContext.forR5Cached()).when(repository).fhirContext();
        final PopulateRequest request =
                newPopulateRequestForVersion(FhirVersionEnum.R5, libraryEngine, originalQuestionnaire);
        final var expectedResponses = getExpectedResponses(request);
        doReturn(expectedResponses).when(fixture).processItems(request, Collections.emptyList());
        // execute
        final IBaseResource actual = fixture.populate(request);
        // validate
        assertEquals(
                prePopulatedQuestionnaireId + "-" + PATIENT_ID,
                actual.getIdElement().getIdPart());
        assertContainedOperationOutcome(request, actual, null);
        assertEquals(questionnaireUrl, request.resolvePathString(actual, "questionnaire"));
        assertEquals(
                org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS,
                ((org.hl7.fhir.r5.model.QuestionnaireResponse) actual).getStatus());
        assertEquals(
                "Patient/" + PATIENT_ID,
                request.resolvePath(actual, "subject", IBaseReference.class)
                        .getReferenceElement()
                        .getValue());
        assertEquals(expectedResponses, request.getItems(actual));
        verify(fixture).processItems(request, Collections.emptyList());
    }

    private List<IBaseBackboneElement> getExpectedResponses(PopulateRequest request) {
        switch (request.getFhirVersion()) {
            case R4:
                return List.of(
                        new QuestionnaireResponseItemComponent(),
                        new QuestionnaireResponseItemComponent(),
                        new QuestionnaireResponseItemComponent());
            case R5:
                return List.of(
                        new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(),
                        new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent(),
                        new org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent());

            default:
                return null;
        }
    }

    private void assertContainedOperationOutcome(
            PopulateRequest request, IBaseResource actual, IBaseOperationOutcome expectedOperationOutcome) {
        final var operationOutcome = getContainedByResourceType(request, actual, "OperationOutcome");
        assertEquals(expectedOperationOutcome, operationOutcome);
    }

    private IBaseResource getContainedByResourceType(
            PopulateRequest request, IBaseResource actual, String resourceType) {
        return request.getContained(actual).stream()
                .filter(c -> c.fhirType().equals(resourceType))
                .findFirst()
                .orElse(null);
    }

    @Test
    void resolveOperationOutcomeShouldAddOperationOutcomeIfHasIssues() {
        // setup
        final OperationOutcome operationOutcome = withOperationOutcomeWithIssue();
        final Questionnaire questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final PopulateRequest request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        request.setOperationOutcome(operationOutcome);
        // execute
        request.resolveOperationOutcome(questionnaire);
        // validate
        assertContainedOperationOutcome(request, questionnaire, operationOutcome);
    }

    @Test
    void resolveOperationOutcomeShouldNotAddOperationOutcomeIfHasNoIssues() {
        // setup
        final OperationOutcome operationOutcome = new OperationOutcome();
        final Questionnaire questionnaire = new Questionnaire();
        doReturn(FhirContext.forR4Cached()).when(repository).fhirContext();
        final PopulateRequest request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        request.setOperationOutcome(operationOutcome);
        // execute
        request.resolveOperationOutcome(questionnaire);
        // validate
        assertContainedOperationOutcome(request, questionnaire, null);
    }

    private OperationOutcome withOperationOutcomeWithIssue() {
        final OperationOutcome operationOutcome = new OperationOutcome();
        operationOutcome
                .addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION);
        return operationOutcome;
    }
}
