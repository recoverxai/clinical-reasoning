package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptReferenceAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class ValueSetConceptReferenceAdapter implements IValueSetConceptReferenceAdapter {

    private final ConceptReferenceComponent conceptReference;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;

    public ValueSetConceptReferenceAdapter(IBaseBackboneElement conceptReference) {
        if (!(conceptReference instanceof ConceptReferenceComponent)) {
            throw new IllegalArgumentException(
                    "element passed as conceptReference argument is not a ConceptReferenceComponent element");
        }
        this.conceptReference = (ConceptReferenceComponent) conceptReference;
        fhirContext = FhirContext.forR4Cached();
        modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R4);
    }

    @Override
    public ConceptReferenceComponent get() {
        return conceptReference;
    }

    @Override
    public FhirContext fhirContext() {
        return fhirContext;
    }

    @Override
    public ModelResolver getModelResolver() {
        return modelResolver;
    }

    @Override
    public boolean hasCode() {
        return get().hasCode();
    }

    @Override
    public String getCode() {
        return get().getCode();
    }
}
