package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.opencds.cqf.fhir.utility.Constants;

public class ExtensionBuilders {
    private ExtensionBuilders() {}

    public static final SimpleEntry<String, String> QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION =
            new SimpleEntry<>(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR, Constants.CQL_ENGINE_DEVICE);

    public static SimpleEntry<String, String> prepopulateSubjectExtension(String patientCode, String patientId) {
        return new SimpleEntry<>(Constants.SDC_QUESTIONNAIRE_PREPOPULATE_SUBJECT, patientCode + "/" + patientId);
    }

    public static SimpleEntry<String, String> crmiMessagesExtension(String operationOutcomeId) {
        return new SimpleEntry<>(Constants.EXT_CRMI_MESSAGES, operationOutcomeId);
    }

    public static SimpleEntry<String, String> dtrQuestionnaireResponseExtension(String questionnaireId) {
        return new SimpleEntry<>(Constants.DTR_QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE, questionnaireId);
    }

    public static SimpleEntry<String, String> pertainToGoalExtension(String goalId) {
        return new SimpleEntry<>(Constants.PERTAINS_TO_GOAL, goalId);
    }

    public static SimpleEntry<String, Boolean> sdcQuestionnaireHidden(Boolean value) {
        return new SimpleEntry<>(Constants.SDC_QUESTIONNAIRE_HIDDEN, value);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBaseExtension<?, ?>> T buildReferenceExt(
            FhirVersionEnum fhirVersion, SimpleEntry<String, String> entry, Boolean isContained) {
        var value = buildReference(fhirVersion, entry.getValue(), isContained);
        switch (fhirVersion) {
            case DSTU3:
                return (T) new org.hl7.fhir.dstu3.model.Extension(entry.getKey(), value);
            case R4:
                return (T) new org.hl7.fhir.r4.model.Extension(entry.getKey(), value);
            case R5:
                return (T) new org.hl7.fhir.r5.model.Extension(entry.getKey(), value);

            default:
                return null;
        }
    }

    public static IBaseReference buildReference(FhirVersionEnum fhirVersion, String id) {
        return buildReference(fhirVersion, id, false);
    }

    public static IBaseReference buildReference(FhirVersionEnum fhirVersion, String id, Boolean isContained) {
        var reference = Boolean.TRUE.equals(isContained) ? "#".concat(id) : id;
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Reference(reference);
            case R4:
                return new org.hl7.fhir.r4.model.Reference(reference);
            case R5:
                return new org.hl7.fhir.r5.model.Reference(reference);

            default:
                return null;
        }
    }

    public static IBaseBooleanDatatype buildBooleanType(FhirVersionEnum fhirVersion, Boolean value) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.BooleanType(value);
            case R4:
                return new org.hl7.fhir.r4.model.BooleanType(value);
            case R5:
                return new org.hl7.fhir.r5.model.BooleanType(value);

            default:
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBaseExtension<?, ?>> T buildBooleanExt(
            FhirVersionEnum fhirVersion, SimpleEntry<String, Boolean> entry) {
        var value = buildBooleanType(fhirVersion, entry.getValue());
        switch (fhirVersion) {
            case DSTU3:
                return (T) new org.hl7.fhir.dstu3.model.Extension(entry.getKey(), value);
            case R4:
                return (T) new org.hl7.fhir.r4.model.Extension(entry.getKey(), value);
            case R5:
                return (T) new org.hl7.fhir.r5.model.Extension(entry.getKey(), value);

            default:
                return null;
        }
    }

    public static <T extends IBaseExtension<?, ?>> T buildSdcLaunchContextExt(
            FhirVersionEnum fhirVersion, String code) {
        return buildSdcLaunchContextExt(fhirVersion, code, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBaseExtension<?, ?>> T buildSdcLaunchContextExt(
            FhirVersionEnum fhirVersion, String code, String resourceType) {
        var system = "http://hl7.org/fhir/uv/sdc/CodeSystem/launchContext";
        var display = "";
        switch (code) {
            case "patient":
                display = "Patient";
                resourceType = display;
                break;
            case "encounter":
                display = "Encounter";
                resourceType = display;
                break;
            case "location":
                display = "Location";
                resourceType = display;
                break;
            case "practitioner":
            case "user":
                display = "User";
                resourceType = resourceType == null ? "Practitioner" : resourceType;
                break;
            case "study":
                display = "ResearchStudy";
                resourceType = display;
                break;

            default:
                throw new IllegalArgumentException(String.format("Unrecognized launch context code: %s", code));
        }
        switch (fhirVersion) {
            case R4:
                return (T) new org.hl7.fhir.r4.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                        .setExtension(Arrays.asList(
                                new org.hl7.fhir.r4.model.Extension(
                                        "name", new org.hl7.fhir.r4.model.Coding(system, code, display)),
                                new org.hl7.fhir.r4.model.Extension(
                                        "type", new org.hl7.fhir.r4.model.CodeType(resourceType))));
            case R5:
                return (T) new org.hl7.fhir.r5.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                        .setExtension(Arrays.asList(
                                new org.hl7.fhir.r5.model.Extension(
                                        "name", new org.hl7.fhir.r5.model.Coding(system, code, display)),
                                new org.hl7.fhir.r5.model.Extension(
                                        "type", new org.hl7.fhir.r5.model.CodeType(resourceType))));

            default:
                return null;
        }
    }
}
