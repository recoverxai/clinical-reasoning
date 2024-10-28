package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DATEOFCOMPLIANCE;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.TOTALDENOMINATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.TOTALNUMERATOR;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CRITERIA_REFERENCE_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SDE_REFERENCE_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_TOTAL_DENOMINATOR_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_TOTAL_NUMERATOR_URL;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.CriteriaResult;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureInfo;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportBuilder;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportScorer;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.SdeDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;

public class R4MeasureReportBuilder implements MeasureReportBuilder<Measure, MeasureReport, DomainResource> {

    protected static final String POPULATION_SUBJECT_SET = "POPULATION_SUBJECT_SET";

    protected MeasureReportScorer<MeasureReport> measureReportScorer;

    public R4MeasureReportBuilder() {
        this.measureReportScorer = new R4MeasureReportScorer();
    }

    private static class BuilderContext {
        private final Measure measure;
        private final MeasureDef measureDef;
        private final MeasureReport measureReport;

        private final HashMap<String, Reference> evaluatedResourceReferences = new HashMap<>();
        private final HashMap<String, Reference> supplementalDataReferences = new HashMap<>();
        private final Map<String, Resource> contained = new HashMap<>();

        public BuilderContext(Measure measure, MeasureDef measureDef, MeasureReport measureReport) {
            this.measure = measure;
            this.measureDef = measureDef;
            this.measureReport = measureReport;
        }

        public Map<String, Resource> contained() {
            return this.contained;
        }

        public void addContained(Resource r) {
            this.contained.putIfAbsent(this.getId(r), r);
        }

        public Measure measure() {
            return this.measure;
        }

        public MeasureReport report() {
            return this.measureReport;
        }

        public MeasureDef measureDef() {
            return this.measureDef;
        }

        public Map<String, Reference> evaluatedResourceReferences() {
            return this.evaluatedResourceReferences;
        }

        public Map<String, Reference> supplementalDataReferences() {
            return this.supplementalDataReferences;
        }

        public Reference addSupplementalDataReference(String id) {
            validateReference(id);
            return this.supplementalDataReferences().computeIfAbsent(id, x -> new Reference(id));
        }

        public Reference addEvaluatedResourceReference(String id) {
            validateReference(id);
            return this.evaluatedResourceReferences().computeIfAbsent(id, x -> new Reference(id));
        }

        public boolean hasEvaluatedResource(String id) {
            validateReference(id);
            return this.evaluatedResourceReferences().containsKey(id);
        }

        public void addCriteriaExtensionToReference(Reference reference, String criteriaId) {
            if (criteriaId == null) throw new AssertionError("CriteriaId is required for extension references");
            var ext = new Extension(EXT_CRITERIA_REFERENCE_URL, new StringType(criteriaId));
            addExtensionIfNotExists(reference, ext);
        }

        public void addCriteriaExtensionToSupplementalData(Resource resource, String criteriaId) {
            var id = getId(resource);

            // This is not an evaluated resource, so add it to the contained resources
            if (!hasEvaluatedResource(id)) {
                this.addContained(resource);
                id = "#" + resource.getIdElement().getIdPart();
            }
            var ref = addSupplementalDataReference(id);
            addCriteriaExtensionToReference(ref, criteriaId);
        }

        public void addCriteriaExtensionToEvaluatedResource(Resource resource, String criteriaId) {
            var id = getId(resource);
            var ref = addEvaluatedResourceReference(id);
            addCriteriaExtensionToReference(ref, criteriaId);
        }

        private String getId(Resource resource) {
            return resource.fhirType() + "/" + resource.getIdElement().getIdPart();
        }

        private void addExtensionIfNotExists(Element element, Extension ext) {
            for (var e : element.getExtension()) {
                if (e.getUrl().equals(ext.getUrl()) && e.getValue().equalsShallow(ext.getValue())) {
                    return;
                }
            }

            element.addExtension(ext);
        }

        private void validateReference(String reference) {
            // Can't be null
            if (reference == null) {
                throw new NullPointerException();
            }

            // If it's a contained reference, must be just the Guid and nothing else
            if (reference.startsWith("#") && reference.contains("/")) {
                throw new IllegalArgumentException();
            }

            // If it's a full reference, it must be type/id and that's it
            if (!reference.startsWith("#") && reference.split("/").length != 2) {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public MeasureReport build(
            Measure measure,
            MeasureDef measureDef,
            MeasureReportType measureReportType,
            Interval measurementPeriod,
            List<String> subjectIds) {

        var report = this.createMeasureReport(measure, measureDef, measureReportType, subjectIds, measurementPeriod);

        var bc = new BuilderContext(measure, measureDef, report);

        // buildGroups must be run first to set up the builder context to be able to use
        // the evaluatedResource references for SDE processing
        buildGroups(bc);

        buildSDEs(bc);

        addEvaluatedResource(bc);
        addSupplementalData(bc);

        for (var r : bc.contained().values()) {
            bc.report().addContained(r);
        }

        this.measureReportScorer.score(measureDef, bc.report());

        return bc.report();
    }

    protected void addSupplementalData(BuilderContext bc) {
        var report = bc.report();

        for (Reference r : bc.supplementalDataReferences().values()) {
            report.addExtension(EXT_SDE_REFERENCE_URL, r);
        }
    }

    protected void addEvaluatedResource(BuilderContext bc) {
        var report = bc.report();
        // Only add evaluated resources to individual reports
        if (report.getType() == org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.INDIVIDUAL) {
            for (Reference r : bc.evaluatedResourceReferences().values()) {
                report.addEvaluatedResource(r);
            }
        }
    }

    protected void buildGroups(BuilderContext bc) {
        var measure = bc.measure();
        var measureDef = bc.measureDef();
        var report = bc.report();

        if (measure.getGroup().size() != measureDef.groups().size()) {
            throw new IllegalArgumentException(
                    "The Measure has a different number of groups defined than the MeasureDef");
        }

        // ASSUMPTION: The groups are in the same order in both the Measure and the
        // MeasureDef
        for (int i = 0; i < measure.getGroup().size(); i++) {
            var measureGroup = measure.getGroup().get(i);
            var defGroup = measureDef.groups().get(i);
            var reportGroup = report.addGroup();
            buildGroup(bc, measureGroup, reportGroup, defGroup);
        }
    }

    private PopulationDef getReportPopulation(GroupDef reportGroup, MeasurePopulationType measurePopType) {
        var populations = reportGroup.populations();
        return populations.stream()
                .filter(e -> e.code().first().code().equals(measurePopType.toCode()))
                .findAny()
                .orElse(null);
    }

    protected void buildGroup(
            BuilderContext bc,
            MeasureGroupComponent measureGroup,
            MeasureReportGroupComponent reportGroup,
            GroupDef groupDef) {

        // groupDef contains populations/stratifier components not defined in measureGroup (TOTAL-NUMERATOR &
        // TOTAL-DENOMINATOR), and will not be added to group populations.
        // Subtracting '2' from groupDef to balance with Measure defined Groups
        var groupDefSizeDiff = 2;
        if (groupDef.populations().stream()
                        .filter(x -> x.type().equals(MeasurePopulationType.DATEOFCOMPLIANCE))
                        .findFirst()
                        .orElse(null)
                != null) {
            // dateOfNonCompliance is another population not calculated
            groupDefSizeDiff = 3;
        }

        if ((measureGroup.getPopulation().size()) != (groupDef.populations().size() - groupDefSizeDiff)) {
            throw new IllegalArgumentException(
                    "The MeasureGroup has a different number of populations defined than the GroupDef");
        }

        if (measureGroup.getStratifier().size() != (groupDef.stratifiers().size())) {
            throw new IllegalArgumentException(
                    "The MeasureGroup has a different number of stratifiers defined than the GroupDef");
        }

        reportGroup.setCode(measureGroup.getCode());
        reportGroup.setId(measureGroup.getId());
        // Measure Level Extension
        addMeasureDescription(reportGroup, measureGroup);
        addExtensionImprovementNotation(reportGroup, bc.measureDef, groupDef);

        for (int i = 0; i < measureGroup.getPopulation().size(); i++) {
            var measurePop = measureGroup.getPopulation().get(i);
            PopulationDef defPop = null;
            for (int x = 0; x < groupDef.populations().size(); x++) {
                var groupDefPop = groupDef.populations().get(x);
                if (groupDefPop
                        .code()
                        .first()
                        .code()
                        .equals(measurePop.getCode().getCodingFirstRep().getCode())) {
                    defPop = groupDefPop;
                    break;
                }
            }
            var reportPop = reportGroup.addPopulation();
            buildPopulation(bc, measurePop, reportPop, defPop);
        }

        // add extension to group for totalDenominator and totalNumerator
        if (groupDef.measureScoring().equals(MeasureScoring.PROPORTION)
                || groupDef.measureScoring().equals(MeasureScoring.RATIO)) {

            // add extension to group for
            if (bc.measureReport.getType().equals(MeasureReport.MeasureReportType.INDIVIDUAL)) {
                var docPopDef = getReportPopulation(groupDef, DATEOFCOMPLIANCE);
                if (docPopDef != null
                        && docPopDef.getResources() != null
                        && !docPopDef.getResources().isEmpty()) {
                    var docValue = docPopDef.getResources().iterator().next();
                    if (docValue != null) {
                        assert docValue instanceof Interval;
                        Interval docInterval = (Interval) docValue;

                        var helper = new R4DateHelper();
                        reportGroup
                                .addExtension()
                                .setUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL)
                                .setValue(helper.buildMeasurementPeriod((docInterval)));
                    }
                }
            }

            if (bc.measureDef.isBooleanBasis()) {
                reportGroup
                        .addExtension()
                        .setUrl(EXT_TOTAL_DENOMINATOR_URL)
                        .setValue(new StringType(Integer.toString(getReportPopulation(groupDef, TOTALDENOMINATOR)
                                .getSubjects()
                                .size())));
                reportGroup
                        .addExtension()
                        .setUrl(EXT_TOTAL_NUMERATOR_URL)
                        .setValue(new StringType(Integer.toString(getReportPopulation(groupDef, TOTALNUMERATOR)
                                .getSubjects()
                                .size())));
            } else {
                reportGroup
                        .addExtension()
                        .setUrl(EXT_TOTAL_DENOMINATOR_URL)
                        .setValue(new StringType(Integer.toString(getReportPopulation(groupDef, TOTALDENOMINATOR)
                                .getResources()
                                .size())));
                reportGroup
                        .addExtension()
                        .setUrl(EXT_TOTAL_NUMERATOR_URL)
                        .setValue(new StringType(Integer.toString(getReportPopulation(groupDef, TOTALNUMERATOR)
                                .getResources()
                                .size())));
            }
        }
        for (int i = 0; i < measureGroup.getStratifier().size(); i++) {
            var groupStrat = measureGroup.getStratifier().get(i);
            var reportStrat = reportGroup.addStratifier();
            var defStrat = groupDef.stratifiers().get(i);
            buildStratifier(bc, groupStrat, reportStrat, defStrat, measureGroup.getPopulation(), groupDef);
        }
    }

    /**
     *
     * Resource result --> Patient Key, Resource result --> can intersect on patient for Boolean basis, can't for Resource
     * boolean result --> Patient Key, Boolean result --> can intersect on Patient
     * code result --> Patient Key, Code result --> can intersect on Patient
     */
    protected void validateStratifierBasisType(Map<String, CriteriaResult> subjectValues, boolean isBooleanBasis) {

        if (!subjectValues.entrySet().isEmpty() && !isBooleanBasis) {
            var list = subjectValues.values().stream()
                    .filter(x -> x.rawValue() instanceof Resource)
                    .collect(Collectors.toList());
            if (list.size() != subjectValues.values().size()) {
                throw new IllegalArgumentException(
                        "stratifier expression criteria results must match the same type as population.");
            }
        }
    }

    protected void buildStratifier(
            BuilderContext bc,
            MeasureGroupStratifierComponent measureStratifier,
            MeasureReportGroupStratifierComponent reportStratifier,
            StratifierDef stratifierDef,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef) {
        reportStratifier.setCode(Collections.singletonList(measureStratifier.getCode()));
        reportStratifier.setId(measureStratifier.getId());

        if (measureStratifier.hasDescription()) {
            reportStratifier.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL,
                    new StringType(measureStratifier.getDescription()));
        }

        Map<String, CriteriaResult> subjectValues = stratifierDef.getResults();

        validateStratifierBasisType(subjectValues, bc.measureDef.isBooleanBasis());

        // Stratifiers should be of the same basis as population
        if (bc.measureDef.isBooleanBasis()) {
            // ValueWrapper is used because most of the types we're dealing with don't implement hashCode or equals
            Map<ValueWrapper, List<String>> subjectsByValue = subjectValues.keySet().stream()
                    .collect(Collectors.groupingBy(
                            x -> new ValueWrapper(subjectValues.get(x).rawValue())));

            for (Map.Entry<ValueWrapper, List<String>> stratValue : subjectsByValue.entrySet()) {
                var reportStratum = reportStratifier.addStratum();
                var patients = stratValue.getValue().stream()
                        .map(t -> ResourceType.Patient.toString().concat("/").concat(t))
                        .collect(Collectors.toList());
                buildStratum(bc, reportStratum, stratValue.getKey(), patients, populations, groupDef);
            }
        } else {
            var values = subjectValues.values().stream()
                    .map(CriteriaResult::rawValue)
                    .filter(Resource.class::isInstance)
                    .map(Resource.class::cast)
                    .map(x -> x.getResourceType().toString().concat("/").concat(x.getIdPart()))
                    .collect(Collectors.toList());
            for (String value : values) {
                buildStratum(
                        bc,
                        reportStratifier.addStratum(),
                        new ValueWrapper(value),
                        Collections.singletonList(value),
                        populations,
                        groupDef);
            }
        }
    }

    protected void addMeasureDescription(MeasureReportGroupComponent reportGroup, MeasureGroupComponent measureGroup) {
        if (measureGroup.hasDescription()) {
            reportGroup.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL, new StringType(measureGroup.getDescription()));
        }
    }

    protected void addExtensionImprovementNotation(
            MeasureReportGroupComponent reportGroup, MeasureDef measureDef, GroupDef groupDef) {
        // if already set on Measure, don't set on groups too
        if (!measureDef.useMeasureImpNotation()) {
            if (groupDef.isPositiveImprovementNotation()) {
                reportGroup.addExtension(
                        MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION,
                        new CodeableConcept(new Coding(
                                MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM,
                                MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE,
                                MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE_DISPLAY)));
            } else {
                reportGroup.addExtension(
                        MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION,
                        new CodeableConcept(new Coding(
                                MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM,
                                MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE,
                                MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE_DISPLAY)));
            }
        }
    }

    protected void buildStratum(
            BuilderContext bc,
            StratifierGroupComponent stratum,
            ValueWrapper value,
            List<String> subjectIds,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef) {

        if (value.getValueClass().equals(CodeableConcept.class)) {
            stratum.setValue((CodeableConcept) value.getValue());
        } else {
            stratum.setValue(new CodeableConcept().setText(value.getValueAsString()));
        }

        for (MeasureGroupPopulationComponent mgpc : populations) {
            var stratumPopulation = stratum.addPopulation();
            buildStratumPopulation(bc, stratumPopulation, subjectIds, mgpc);
        }

        // add totalDenominator and totalNumerator extensions
        buildStratumExtPopulation(
                groupDef,
                TOTALDENOMINATOR,
                subjectIds,
                stratum,
                EXT_TOTAL_DENOMINATOR_URL,
                bc.measureDef.isBooleanBasis());
        buildStratumExtPopulation(
                groupDef, TOTALNUMERATOR, subjectIds, stratum, EXT_TOTAL_NUMERATOR_URL, bc.measureDef.isBooleanBasis());
    }

    protected void buildStratumExtPopulation(
            GroupDef groupDef,
            MeasurePopulationType measurePopulationType,
            List<String> subjectIds,
            StratifierGroupComponent stratum,
            String extUrl,
            boolean isBooleanBasis) {
        Set<String> subjectPop;
        var reportPopulation = getReportPopulation(groupDef, measurePopulationType);
        assert reportPopulation != null;
        if (isBooleanBasis) {
            subjectPop = reportPopulation.getSubjects().stream()
                    .map(t -> ResourceType.Patient.toString().concat("/").concat(t))
                    .collect(Collectors.toSet());
        } else {
            subjectPop = reportPopulation.getResources().stream()
                    .filter(Resource.class::isInstance)
                    .map(Resource.class::cast)
                    .map(x -> x.getResourceType().toString().concat("/").concat(x.getIdPart()))
                    .collect(Collectors.toSet());
        }
        int count;

        Set<String> intersection = new HashSet<>(subjectIds);
        intersection.retainAll(subjectPop);
        count = intersection.size();
        stratum.addExtension().setUrl(extUrl).setValue(new StringType(Integer.toString(count)));
    }

    protected void buildStratumPopulation(
            BuilderContext bc,
            StratifierGroupPopulationComponent sgpc,
            List<String> subjectIds,
            MeasureGroupPopulationComponent population) {
        sgpc.setCode(population.getCode());
        sgpc.setId(population.getId());

        if (population.hasDescription()) {
            sgpc.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL, new StringType(population.getDescription()));
        }

        // This is a temporary resource that was carried by the population component
        @SuppressWarnings("unchecked")
        Set<String> popSubjectIds = (Set<String>) population.getUserData(POPULATION_SUBJECT_SET);

        if (popSubjectIds == null) {
            sgpc.setCount(0);
            return;
        }

        Set<String> intersection = new HashSet<>(subjectIds);
        intersection.retainAll(popSubjectIds);
        sgpc.setCount(intersection.size());

        if (!intersection.isEmpty()
                && bc.report().getType() == org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUBJECTLIST) {
            ListResource popSubjectList = this.createIdList(UUID.randomUUID().toString(), intersection);
            bc.addContained(popSubjectList);
            sgpc.setSubjectResults(new Reference("#" + popSubjectList.getId()));
        }
    }

    protected String getPopulationResourceIds(Object resourceObject) {
        var resource = (Resource) resourceObject;
        return resource.getId();
    }

    protected void buildPopulation(
            BuilderContext bc,
            MeasureGroupPopulationComponent measurePopulation,
            MeasureReportGroupPopulationComponent reportPopulation,
            PopulationDef populationDef) {

        reportPopulation.setCode(measurePopulation.getCode());
        reportPopulation.setId(measurePopulation.getId());

        if (bc.measureDef.isBooleanBasis()) {
            reportPopulation.setCount(populationDef.getSubjects().size());
        } else {
            reportPopulation.setCount(populationDef.getResources().size());
        }

        if (measurePopulation.hasDescription()) {
            reportPopulation.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL,
                    new StringType(measurePopulation.getDescription()));
        }

        addEvaluatedResourceReferences(bc, populationDef.id(), populationDef.getEvaluatedResources());

        // This is a temporary list carried forward to stratifiers
        // subjectResult set defined by basis of Measure
        Set<String> populationSet;
        if (bc.measureDef.isBooleanBasis()) {
            populationSet = populationDef.getSubjects().stream()
                    .map(t -> ResourceType.Patient.toString().concat("/").concat(t))
                    .collect(Collectors.toSet());
        } else {
            populationSet = populationDef.getResources().stream()
                    .filter(Resource.class::isInstance)
                    .map(this::getPopulationResourceIds)
                    .collect(Collectors.toSet());
        }

        measurePopulation.setUserData(POPULATION_SUBJECT_SET, populationSet);

        // Report Type behavior
        if (Objects.requireNonNull(bc.report().getType()) == MeasureReport.MeasureReportType.SUBJECTLIST
                && !populationSet.isEmpty()) {
            ListResource subjectList = createIdList(UUID.randomUUID().toString(), populationSet);
            bc.addContained(subjectList);
            reportPopulation.setSubjectResults(new Reference("#" + subjectList.getId()));
        }

        // Population Type behavior
        if (Objects.requireNonNull(populationDef.type()) == MeasurePopulationType.MEASUREOBSERVATION) {
            buildMeasureObservations(bc, populationDef.expression(), populationDef.getResources());
        }
    }

    protected void buildMeasureObservations(BuilderContext bc, String observationName, Set<Object> resources) {
        for (int i = 0; i < resources.size(); i++) {
            // TODO: Do something with the resource...
            Observation observation = createMeasureObservation(
                    bc, "measure-observation-" + observationName + "-" + (i + 1), observationName);
            bc.addContained(observation);
        }
    }

    protected ListResource createList(String id) {
        ListResource list = new ListResource();
        list.setId(id);
        return list;
    }

    protected ListResource createIdList(String id, Collection<String> ids) {
        return this.createReferenceList(id, ids.stream().map(Reference::new).collect(Collectors.toList()));
    }

    protected ListResource createReferenceList(String id, Collection<Reference> references) {
        ListResource referenceList = createList(id);
        for (Reference reference : references) {
            referenceList.addEntry().setItem(reference);
        }

        return referenceList;
    }

    protected void addEvaluatedResourceReferences(
            BuilderContext bc, String criteriaId, Set<Object> evaluatedResources) {
        if (evaluatedResources == null || evaluatedResources.isEmpty()) {
            return;
        }

        for (Object object : evaluatedResources) {
            Resource resource = (Resource) object;
            bc.addCriteriaExtensionToEvaluatedResource(resource, criteriaId);
        }
    }

    // This processes the SDEs for a given report.
    // Case 1: individual - primitive types (ints, codes, etc)
    // convert to observation, add observation as contained, add sde reference with
    // criteria reference extension
    // Case 2: individual - resource types
    // add sde reference with criteria reference extension for each resource
    // if not an evaluated resource, add to contained
    // Case 3: population - primitive types, non aggregatable
    // convert to observation, add observation as contained, add sde reference with
    // criteria reference extension,
    // Case 4: population - primitive type, aggregatable
    // aggregate by value, convert to observation, add observation as contained, sum
    // the
    // sde reference with criteria reference extension
    // Case 5: population - resource types
    // add sde reference with criteria reference extension for each resource
    // if not an evaluated resource, add to contained
    protected void buildSDE(BuilderContext bc, SdeDef sde) {
        var report = bc.report();

        // No SDEs were calculated, do nothing
        if (sde.getResults().isEmpty()) {
            return;
        }

        // This is an individual report... shouldn't have more than one subject!
        if (report.getType() == MeasureReport.MeasureReportType.INDIVIDUAL
                && sde.getResults().keySet().size() > 1) {
            throw new IllegalArgumentException();
        }

        // Add all evaluated resources
        for (var e : sde.getResults().entrySet()) {
            addEvaluatedResourceReferences(bc, sde.id(), e.getValue().evaluatedResources());
        }

        CodeableConcept concept = conceptDefToConcept(sde.code());

        Map<ValueWrapper, Long> accumulated = sde.getResults().values().stream()
                .flatMap(x -> Lists.newArrayList(x.iterableValue()).stream())
                .filter(Objects::nonNull)
                .map(ValueWrapper::new)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        for (Map.Entry<ValueWrapper, Long> accumulator : accumulated.entrySet()) {

            Resource obs;
            if (!(accumulator.getKey().getValue() instanceof Resource)) {
                String valueCode = accumulator.getKey().getValueAsString();
                Long valueCount = accumulator.getValue();

                Coding valueCoding = new Coding().setCode(valueCode);

                if (Objects.requireNonNull(report.getType()) == MeasureReport.MeasureReportType.INDIVIDUAL) {
                    obs = createPatientObservation(bc, UUID.randomUUID().toString(), sde.id(), valueCoding, concept);
                } else {
                    obs = createPopulationObservation(
                            bc, UUID.randomUUID().toString(), sde.id(), valueCoding, valueCount, concept);
                }

                bc.addCriteriaExtensionToSupplementalData(obs, sde.id());
            } else {
                Resource r = (Resource) accumulator.getKey().getValue();
                bc.addCriteriaExtensionToSupplementalData(r, sde.id());
            }
        }
    }

    protected void buildSDEs(BuilderContext bc) {
        var measure = bc.measure();
        var measureDef = bc.measureDef();
        // ASSUMPTION: Measure SDEs are in the same order as MeasureDef SDEs
        for (int i = 0; i < measure.getSupplementalData().size(); i++) {
            var sde = measureDef.sdes().get(i);
            buildSDE(bc, sde);
        }
    }

    private CodeableConcept conceptDefToConcept(ConceptDef c) {
        var cc = new CodeableConcept().setText(c.text());
        for (var cd : c.codes()) {
            cc.addCoding(codeDefToCoding(cd));
        }

        return cc;
    }

    private Coding codeDefToCoding(CodeDef c) {
        var cd = new Coding();
        cd.setSystem(c.system());
        cd.setCode(c.code());
        cd.setVersion(c.version());
        cd.setDisplay(c.display());

        return cd;
    }

    protected MeasureReport createMeasureReport(
            Measure measure,
            MeasureDef measureDef,
            MeasureReportType type,
            List<String> subjectIds,
            Interval measurementPeriod) {
        MeasureReport report = new MeasureReport();
        report.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
        report.setType(org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.fromCode(type.toCode()));

        if (type == MeasureReportType.INDIVIDUAL && !subjectIds.isEmpty()) {
            report.setSubject(new Reference(subjectIds.get(0)));
        }
        var helper = new R4DateHelper();
        if (measurementPeriod != null) {
            report.setPeriod(helper.buildMeasurementPeriod((measurementPeriod)));
        }

        report.setMeasure(getMeasure(measure));
        report.setDate(new java.util.Date());
        report.setImplicitRules(measure.getImplicitRules());
        if (measureDef.useMeasureImpNotation()) {
            // if true, all group components have the same improvement Notation
            report.setImprovementNotation(measure.getImprovementNotation());
        }
        report.setLanguage(measure.getLanguage());

        if (measure.hasDescription()) {
            report.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL, new StringType(measure.getDescription()));
        }

        return report;
    }

    protected Extension createMeasureInfoExtension(MeasureInfo measureInfo) {

        Extension extExtMeasure =
                new Extension().setUrl(MeasureInfo.MEASURE).setValue(new CanonicalType(measureInfo.getMeasure()));

        Extension obsExtension = new Extension().setUrl(MeasureInfo.EXT_URL);
        obsExtension.addExtension(extExtMeasure);

        return obsExtension;
    }

    private String getMeasure(Measure measure) {
        if (StringUtils.isNotBlank(measure.getUrl()) && !measure.getUrl().contains("|") && measure.hasVersion()) {
            return measure.getUrl() + "|" + measure.getVersion();
        }
        return measure.getUrl();
    }

    private Coding supplementalDataCoding;

    private Coding geSupplementalDataCoding() {
        if (supplementalDataCoding == null) {
            supplementalDataCoding = new Coding()
                    .setCode("supplemental-data")
                    .setSystem("http://terminology.hl7.org/CodeSystem/measure-data-usage");
        }
        return supplementalDataCoding;
    }

    private CodeableConcept getMeasureUsageConcept(CodeableConcept originalConcept) {
        CodeableConcept measureUsageConcept = new CodeableConcept();
        List<Coding> list = new ArrayList<>();
        list.add(geSupplementalDataCoding());
        measureUsageConcept.setCoding(list);

        if (originalConcept != null) {
            if (originalConcept.hasText() && StringUtils.isNotBlank(originalConcept.getText())) {
                measureUsageConcept.setText(originalConcept.getText());
            }
            if (originalConcept.hasCoding()) {
                measureUsageConcept.getCoding().add(originalConcept.getCodingFirstRep());
            }
        }
        return measureUsageConcept;
    }

    protected DomainResource createPopulationObservation(
            BuilderContext bc,
            String id,
            String populationId,
            Coding valueCoding,
            Long sdeAccumulatorValue,
            CodeableConcept originalConcept) {

        Observation obs = createObservation(bc, id, populationId);

        CodeableConcept obsCodeableConcept = new CodeableConcept();
        List<Coding> list = new ArrayList<>();
        list.add(valueCoding);
        if (originalConcept != null && originalConcept.hasCoding()) {
            list.add(originalConcept.getCodingFirstRep());
        }
        obsCodeableConcept.setCoding(list);

        obs.setCode(obsCodeableConcept);
        obs.setValue(new IntegerType(sdeAccumulatorValue));

        return obs;
    }

    protected DomainResource createPatientObservation(
            BuilderContext bc, String id, String populationId, Coding valueCoding, CodeableConcept originalConcept) {

        Observation obs = createObservation(bc, id, populationId);

        obs.setCode(getMeasureUsageConcept(originalConcept));

        CodeableConcept valueCodeableConcept = new CodeableConcept();
        valueCodeableConcept.setCoding(Collections.singletonList(valueCoding));
        obs.setValue(valueCodeableConcept);
        return obs;
    }

    protected Observation createObservation(BuilderContext bc, String id, String populationId) {
        var measure = bc.measure();
        MeasureInfo measureInfo = new MeasureInfo()
                .withMeasure(
                        measure.hasUrl()
                                ? measure.getUrl()
                                : (measure.hasId()
                                        ? MeasureInfo.MEASURE_PREFIX
                                                + measure.getIdElement().getIdPart()
                                        : ""))
                .withPopulationId(populationId);

        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setId(id);
        obs.addExtension(createMeasureInfoExtension(measureInfo));

        return obs;
    }

    protected Observation createMeasureObservation(BuilderContext bc, String id, String observationName) {
        Observation obs = this.createObservation(bc, id, observationName);
        CodeableConcept cc = new CodeableConcept();
        cc.setText(observationName);
        obs.setCode(cc);
        return obs;
    }

    // This is some hackery because most of these objects don't implement
    // hashCode or equals, meaning it's hard to detect distinct values;
    class ValueWrapper {
        protected Object value;

        public ValueWrapper(Object value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return this.getKey().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (this.getClass() != o.getClass()) return false;

            ValueWrapper other = (ValueWrapper) o;

            if (other.getValue() == null ^ this.getValue() == null) {
                return false;
            }

            if (other.getValue() == null && this.getValue() == null) {
                return true;
            }

            return this.getKey().equals(other.getKey());
        }

        public String getKey() {
            String key = null;
            if (value instanceof Coding) {
                Coding c = ((Coding) value);
                // ASSUMPTION: We won't have different systems with the same code
                // within a given stratifier / sde
                key = joinValues("coding", c.getCode());
            } else if (value instanceof CodeableConcept) {
                CodeableConcept c = ((CodeableConcept) value);
                key = joinValues("codeable-concept", c.getCodingFirstRep().getCode());
            } else if (value instanceof Code) {
                Code c = (Code) value;
                key = joinValues("code", c.getCode());
            } else if (value instanceof Enum) {
                Enum<?> e = (Enum<?>) value;
                key = joinValues("enum", e.toString());
            } else if (value instanceof IPrimitiveType<?>) {
                IPrimitiveType<?> p = (IPrimitiveType<?>) value;
                key = joinValues("primitive", p.getValueAsString());
            } else if (value instanceof Identifier) {
                key = ((Identifier) value).getValue();
            } else if (value instanceof Resource) {
                key = ((Resource) value).getIdElement().toVersionless().getValue();
            } else if (value != null) {
                key = value.toString();
            }

            if (key == null) {
                throw new IllegalArgumentException(String.format("found a null key for the wrapped value: %s", value));
            }

            return key;
        }

        public String getValueAsString() {
            if (value instanceof Coding) {
                Coding c = ((Coding) value);
                return c.getCode();
            } else if (value instanceof CodeableConcept) {
                CodeableConcept c = ((CodeableConcept) value);
                return c.getCodingFirstRep().getCode();
            } else if (value instanceof Code) {
                Code c = (Code) value;
                return c.getCode();
            } else if (value instanceof Enum) {
                Enum<?> e = (Enum<?>) value;
                return e.toString();
            } else if (value instanceof IPrimitiveType<?>) {
                IPrimitiveType<?> p = (IPrimitiveType<?>) value;
                return p.getValueAsString();
            } else if (value instanceof Identifier) {
                return ((Identifier) value).getValue();
            } else if (value instanceof Resource) {
                return ((Resource) value).getIdElement().toVersionless().getValue();
            } else if (value != null) {
                return value.toString();
            } else {
                return "<null>";
            }
        }

        public String getDescription() {
            if (value instanceof Coding) {
                Coding c = ((Coding) value);
                return c.hasDisplay() ? c.getDisplay() : c.getCode();
            } else if (value instanceof CodeableConcept) {
                CodeableConcept c = ((CodeableConcept) value);
                return c.getCodingFirstRep().hasDisplay()
                        ? c.getCodingFirstRep().getDisplay()
                        : c.getCodingFirstRep().getCode();
            } else if (value instanceof Code) {
                Code c = (Code) value;
                return c.getDisplay() != null ? c.getDisplay() : c.getCode();
            } else if (value instanceof Enum) {
                Enum<?> e = (Enum<?>) value;
                return e.toString();
            } else if (value instanceof IPrimitiveType<?>) {
                IPrimitiveType<?> p = (IPrimitiveType<?>) value;
                return p.getValueAsString();
            } else if (value instanceof Identifier) {
                return ((Identifier) value).getValue();
            } else if (value instanceof Resource) {
                return ((Resource) value).getIdElement().toVersionless().getValue();
            } else if (value != null) {
                return value.toString();
            } else {
                return null;
            }
        }

        public Object getValue() {
            return this.value;
        }

        public Class<?> getValueClass() {
            if (this.value == null) {
                return String.class;
            }

            return this.value.getClass();
        }

        private String joinValues(String... elements) {
            return String.join("-", elements);
        }
    }
}
